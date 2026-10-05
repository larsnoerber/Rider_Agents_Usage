globalThis.__agentMeterReaders ||= {};
globalThis.__agentMeterReaders.copilot = async page => {
    if (page.origin !== "https://github.com") return {error: "unsupported"};
    const {readDom, request, parseHtml} = page;
    const specs = [
        {
            title: "Included credits",
            label: /^(?:included credits|enthaltene credits|inkludierte credits|enthaltene guthaben)(?:\s|$)/i,
            consumed: true
        },
        {
            title: "Premium",
            label: /^(?:premium requests|premium interactions|(?:GitHub\s+)?AI credits|premium-anfragen|(?:GitHub\s+)?KI-credits)(?:\s|$)/i,
            consumed: true
        },
        {title: "Chat", label: /^(?:chat requests|chat-anfragen)(?:\s|$)/i, consumed: true},
        {
            title: "Inline suggestions",
            label: /^(?:inline suggestions|inline-vorschläge|inline vorschläge|inlinevorschläge)(?:\s|$)/i,
            consumed: true
        }
    ];
    const plans = ["Free", "Pro", "Pro+", "Student", "Business", "Enterprise"];

    function readCounts(root) {
        const quotas = [];
        for (const spec of specs) {
            const values = [];
            for (const label of root.querySelectorAll("h1,h2,h3,h4,p,span,div,label")) {
                const text = (label.innerText || label.textContent || "").replace(/\s+/g, " ").trim();
                if (text.length > 100 || label.children.length > 3 || !spec.label.test(text)) continue;
                let container = label;
                for (let depth = 0; depth < 6 && container; depth++, container = container.parentElement) {
                    if (["MAIN", "BODY", "HTML"].includes(container.tagName)) break;
                    const context = (container.innerText || container.textContent || "").replace(/\s+/g, " ").trim();
                    if (context.length > 500) break;
                    if (/[$€£%]|remaining|available|verbleib|verfügbar/i.test(context)) continue;
                    // Only explicit count pairs for this recognized quota category.
                    // Do not infer an allowance from prices, plans or activity totals.
                    const patterns = [
                        /\b(\d+)\s*(?:\/|of|out of|von)\s*(\d+)\s*(?:(?:premium |chat )?(?:requests|interactions|credits|anfragen)\s+)?(?:used|consumed|verbraucht|genutzt)\b/gi,
                        /\b(?:used|consumed|verbraucht|genutzt)\s*:?\s*(\d+)\s*(?:\/|of|out of|von)\s*(\d+)\b/gi,
                        /\b(\d+)\s+(?:used|consumed|verbraucht|genutzt)\s*(?:\/|of|out of|von)\s*(\d+)\b/gi
                    ];
                    const matches = patterns.flatMap(pattern => [...context.matchAll(pattern)]);
                    if (matches.length !== 1) continue;
                    const used = Number(matches[0][1]), total = Number(matches[0][2]);
                    if (!Number.isSafeInteger(used) || !Number.isSafeInteger(total) || total <= 0 || used > total) continue;
                    values.push(used * 100 / total);
                    break;
                }
            }
            const unique = [...new Set(values)];
            if (unique.length === 1) quotas.push({title: spec.title, percent: unique[0], consumed: true});
        }
        return {quotas};
    }

    function readEmbedded(root) {
        const readings = [];
        for (const element of root.querySelectorAll('script[type="application/json"]')) {
            if ((element.textContent || "").length > 500000) continue;
            try {
                let value = JSON.parse(element.textContent);
                for (let depth = 0; depth < 5 && value && typeof value === "object"; depth++) {
                    const snapshots = value.quota_snapshots;
                    if (snapshots && typeof snapshots === "object") {
                        const quotas = [];
                        for (const [key, title] of [["premium_interactions", "Premium"], ["chat", "Chat"]]) {
                            const quota = snapshots[key];
                            if (!quota || quota.unlimited === true || quota.entitlement === 0) continue;
                            const percent = quota.percent_remaining;
                            if (typeof percent !== "number" || !Number.isFinite(percent) || percent < 0 || percent > 100) continue;
                            quotas.push({title, percent: 100 - percent, consumed: true});
                        }
                        if (quotas.length) readings.push({quotas});
                        break;
                    }
                    value = value.payload || value.data || value.props;
                }
            } catch { /* Unsupported embedded data stays unavailable. */
            }
        }
        return readings.length === 1 ? readings[0] : {quotas: []};
    }

    function readPage(root) {
        const percent = readDom(specs, plans, root);
        if (percent.quotas.length) return percent;
        const embedded = readEmbedded(root);
        if (embedded.quotas.length) return embedded;
        const counts = readCounts(root);
        return counts.quotas.length ? counts : percent;
    }

    let result = {error: "unsupported"};
    const visited = new Set();

    async function readFragments(root) {
        // Read references supplied by GitHub itself, never invented API routes.
        // Scope them to its personal quota settings; ignore other URLs and resources.
        for (const fragment of root.querySelectorAll("include-fragment[src],include-fragment[data-agentmeter-src]")) {
            if (visited.size >= 4) break;
            let url;
            try {
                url = new URL(fragment.getAttribute("data-agentmeter-src") || fragment.getAttribute("src"), page.origin);
            } catch {
                continue;
            }
            if (url.origin !== page.origin ||
                !/^\/settings\/(?:copilot|billing)\/(?:[a-z0-9_-]+\/)*(?:usage|quota|credits|premium_requests|premium_requests_usage|usage_summary)\/?$/i.test(url.pathname)) continue;
            const path = url.pathname + url.search;
            if (visited.has(path)) continue;
            visited.add(path);
            try {
                const reading = readPage(parseHtml(await request(path, {headers: {Accept: "text/html"}})));
                if (reading.quotas.length) return reading;
            } catch (failure) {
                if (failure?.message === "rateLimited") throw failure;
            }
        }
        return null;
    }

    if (/^\/settings\/(?:copilot(?:\/features)?|billing(?:\/(?:usage|premium_requests|summary))?)\/?$/.test(page.pathname)) {
        result = readPage(document);
        if (result.quotas.length) return result;
        try {
            const fragment = await readFragments(document);
            if (fragment) return fragment;
        } catch (failure) {
            if (failure?.message === "rateLimited") return {
                error: "rateLimited",
                diagnostic: {stage: "copilotPages", status: failure.status}
            };
        }
    }
    // Read only fixed billing pages. Detached HTML is never inserted or executed.
    for (const path of ["/settings/copilot/features", "/settings/billing", "/settings/billing/usage"]) {
        try {
            const root = parseHtml(await request(path));
            const reading = readPage(root);
            if (reading.quotas.length) return reading;
            const fragment = await readFragments(root);
            if (fragment) return fragment;
            result = reading;
        } catch (failure) {
            if (failure?.message === "rateLimited") return {
                error: "rateLimited",
                diagnostic: {stage: "copilotPages", status: failure.status}
            };
            // Preserve a useful parsed-page error when another supported URL is unavailable.
            if (!["noLabels", "noPercentages", "noDirection"].includes(result.error))
                result = {
                    error: failure?.message === "rejected" ? "rejected" : "failed",
                    diagnostic: {stage: "copilotPages", status: failure?.status}
                };
        }
    }
    return {...result, diagnostic: result.diagnostic || {stage: "copilotPages"}};
};
globalThis.__agentMeterPage ? globalThis.__agentMeterReaders.copilot(globalThis.__agentMeterPage) : undefined;
