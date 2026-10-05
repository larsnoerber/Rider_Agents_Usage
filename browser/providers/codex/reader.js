globalThis.__agentMeterReaders ||= {};
globalThis.__agentMeterReaders.codex = async page => {
    if (page.origin !== "https://chatgpt.com") return {error: "unsupported"};
    const {request, readDom} = page;
    let error = "unsupported";
    let stage = "session";
    let status = null;
    try {
        // The website owns this session. Keep its token only inside this isolated reader,
        // and use it only for the same-origin, read-only Codex quota request.
        let session = JSON.parse(await request("/api/auth/session"));
        let token = session?.accessToken;
        session = null;
        if (typeof token !== "string" || !token.length) throw new Error("rejected");
        let raw;
        try {
            const headers = {Authorization: `Bearer ${token}`};
            // Codex supports workspace routing via this header. Use only the account
            // explicitly bound to the official session token; never guess an account.
            try {
                const segment = token.split(".")[1];
                const payload = JSON.parse(atob(segment.replace(/-/g, "+").replace(/_/g, "/")));
                const account = payload?.["https://api.openai.com/auth"]?.chatgpt_account_id;
                if (typeof account === "string" && /^[a-zA-Z0-9_-]{1,100}$/.test(account)) headers["ChatGPT-Account-Id"] = account;
            } catch { /* Opaque website tokens remain supported. */
            }
            stage = "codexUsage";
            raw = await request("/backend-api/wham/usage", {headers});
        } finally {
            token = null;
        }
        const value = JSON.parse(raw);
        raw = null;
        const quotas = [];
        for (const window of [value?.rate_limit?.primary_window, value?.rate_limit?.secondary_window]) {
            const used = window?.used_percent;
            const seconds = window?.limit_window_seconds;
            const title = seconds === 18000 ? "Session" : seconds === 604800 ? "Weekly" : null;
            if (!title || typeof used !== "number" || !Number.isFinite(used) || used < 0 || used > 100) continue;
            const reset = typeof window.reset_at === "number" ? window.reset_at * 1000 : NaN;
            quotas.push({
                title, percent: 100 - used, consumed: false,
                resetsAt: Number.isFinite(reset) && reset > 0 ? new Date(reset).toISOString() : null
            });
        }
        const plans = {
            free: "Free",
            plus: "Plus",
            pro: "Pro",
            team: "Team",
            business: "Business",
            enterprise: "Enterprise"
        };
        if (quotas.length) return {quotas, plan: plans[value.plan_type] || null};
    } catch (failure) {
        error = ["rejected", "rateLimited"].includes(failure?.message) ? failure.message : "failed";
        status = failure?.status;
    }
    if (!/^\/codex\/(?:cloud\/)?settings\/(?:usage|analytics)\/?$/.test(page.pathname)) return {
        error,
        diagnostic: {stage, status}
    };
    const fallback = readDom([
        {
            title: "Session",
            label: /^(?:(?:5[-\s]?hour|five[-\s]?hour)(?:[\s-]+(?:usage|limit))?|daily(?:\s+(?:usage|limit))?|session(?:\s+(?:usage|limit))?|5[-\s]?stunden(?:[\s-]+(?:nutzungs)?limit)?|(?:nutzungs)?limit\s+(?:für|pro)\s+5\s+stunden|tägliches\s+(?:nutzungs)?limit)(?:\s|$)/i,
            consumed: false
        },
        {
            title: "Weekly",
            label: /^(?:weekly(?:\s+(?:usage|limit))?|wöchentlich(?:es)?(?:\s+(?:nutzungs)?limit)?|(?:nutzungs)?limit\s+pro\s+woche)(?:\s|$)/i,
            consumed: false
        }
    ], ["Free", "Plus", "Pro", "Team", "Business", "Enterprise"]);
    return fallback.quotas.length ? fallback : {error, diagnostic: {stage, status}};
};
globalThis.__agentMeterPage ? globalThis.__agentMeterReaders.codex(globalThis.__agentMeterPage) : undefined;
