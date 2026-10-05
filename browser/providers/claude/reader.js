globalThis.__agentMeterReaders ||= {};
globalThis.__agentMeterReaders.claude = async page => {
    if (page.origin !== "https://claude.ai") return {error: "unsupported"};
    const {request, readDom, parseHtml} = page;
    const specs = [
        {
            title: "Session",
            label: /^(?:current session|session usage|aktuelle sitzung|aktuelle session)(?:\s|$)/i,
            consumed: false
        },
        {
            title: "Weekly",
            label: /^(?:all models|weekly limits?|alle modelle|wöchentliche limits?)(?:\s|$)/i,
            consumed: false
        },
        {title: "Sonnet weekly", label: /^(?:sonnet only|nur sonnet)(?:\s|$)/i, consumed: false},
        {title: "Opus weekly", label: /^(?:opus only|nur opus)(?:\s|$)/i, consumed: false}
    ];
    const plans = ["Free", "Pro", "Max", "Max 5x", "Max 20x", "Team", "Enterprise"];
    let error = "unsupported";
    let stage = "claudeOrganizations";
    let status = null;
    try {
        const organizations = JSON.parse(await request("/api/organizations"));
        if (!Array.isArray(organizations)) throw new Error("failed");
        // Match the website's selected organization when present; never pick an
        // arbitrary account if multiple organizations are available.
        const selected = page.selectedOrganization?.();
        const valid = organizations.filter(org => typeof org?.uuid === "string" &&
            /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(org.uuid));
        const chat = valid.filter(org => Array.isArray(org.capabilities) && org.capabilities.includes("chat"));
        // Claude chat and Anthropic Console can each return an organization. Console
        // organizations do not own the subscription quota and must not block selection.
        const eligible = chat.length ? chat : valid.filter(org => !Array.isArray(org.capabilities));
        const org = eligible.find(org => org.uuid === selected) || (eligible.length === 1 ? eligible[0] : null);
        if (!org) throw new Error(eligible.length ? "organizationRequired" : "rejected");
        stage = "claudeUsage";
        const value = JSON.parse(await request(`/api/organizations/${org.uuid}/usage`, {headers: {Accept: "application/json"}}));
        if (value?.error) throw new Error("rejected");
        const quotas = [];
        let known = false;
        for (const [key, title] of [["five_hour", "Session"], ["seven_day", "Weekly"],
            ["seven_day_sonnet", "Sonnet weekly"], ["seven_day_opus", "Opus weekly"]]) {
            const window = value?.[key];
            if (value && Object.hasOwn(value, key)) known = true;
            const raw = window?.utilization;
            const used = typeof raw === "string" && /^\d+(?:\.\d+)?$/.test(raw) ? Number(raw) : raw;
            if (typeof used !== "number" || !Number.isFinite(used) || used < 0 || used > 100) continue;
            const reset = typeof window.resets_at === "string" ? Date.parse(window.resets_at) : NaN;
            quotas.push({
                title, percent: 100 - used, consumed: false,
                resetsAt: Number.isFinite(reset) ? new Date(reset).toISOString() : null
            });
        }
        if (quotas.length) return {quotas};
        error = known ? "claudeNoQuota" : "quotaFormat";
    } catch (failure) {
        error = ["rejected", "rateLimited", "organizationRequired"].includes(failure?.message) ? failure.message : "failed";
        status = failure?.status;
    }
    if (/^\/settings\/usage\/?$/.test(page.pathname)) {
        const fallback = readDom(specs, plans);
        if (fallback.quotas.length) return fallback;
    }
    if (!["rateLimited", "organizationRequired"].includes(error)) {
        try {
            const fallback = readDom(specs, plans, parseHtml(await request("/settings/usage")));
            if (fallback.quotas.length) return fallback;
        } catch (failure) {
            if (failure?.message === "rateLimited") return {
                error: "rateLimited",
                diagnostic: {stage, status: failure.status}
            };
        }
    }
    return {error, diagnostic: {stage, status}};
};
globalThis.__agentMeterPage ? globalThis.__agentMeterReaders.claude(globalThis.__agentMeterPage) : undefined;
