globalThis.__agentMeterReaders ||= {};
globalThis.__agentMeterReaders.cursor = async page => {
    if (page.origin !== "https://cursor.com") return {error: "unsupported"};
    try {
        const response = JSON.parse(await page.request("/api/usage-summary"));
        const individual = response.individualUsage;
        const plan = individual?.plan || response.planUsage;
        const pooled = response.teamUsage?.pooled;
        const finite = value => typeof value === "number" && Number.isFinite(value);
        const quota = [plan, individual?.overall, pooled].find(value => value &&
            (finite(value.totalPercentUsed) || (finite(value.limit) && value.limit > 0 && (finite(value.used) || finite(value.remaining)))));
        const quotas = [];
        const used = finite(quota?.used) ? quota.used : quota?.limit - quota?.remaining;
        const percent = finite(quota?.totalPercentUsed) ? quota.totalPercentUsed : used * 100 / quota?.limit;
        const rawReset = response.billingCycleEnd;
        const date = typeof rawReset === "number" ? rawReset : typeof rawReset === "string" && /^\d{13}$/.test(rawReset) ? Number(rawReset) : Date.parse(rawReset);
        if (finite(percent) && percent >= 0 && percent <= 100) quotas.push({
            title: quota === pooled ? "Shared team pool" : "Included plan",
            percent, consumed: true, resetsAt: Number.isFinite(date) ? new Date(date).toISOString() : null
        });
        for (const [key, title] of [["autoPercentUsed", "Auto"], ["apiPercentUsed", "API"]])
            if (finite(plan?.[key]) && plan[key] >= 0 && plan[key] <= 100) quotas.push({
                title,
                percent: plan[key],
                consumed: true
            });
        if (quotas.length) {
            const membership = String(response.membershipType || "").toLowerCase();
            const name = {
                free: "Free",
                pro: "Pro",
                "pro+": "Pro+",
                teams: "Teams",
                enterprise: "Enterprise",
                ultra: "Ultra"
            }[membership];
            return {quotas, plan: name || null};
        }
    } catch (error) {
        if (["rejected", "rateLimited"].includes(error.message)) return {error: error.message};
    }
    return page.readDom([
        {title: "Included plan", label: /^(?:included usage|included plan|plan usage)(?:\s|$)/i, consumed: true},
        {title: "Auto", label: /^auto(?:\s|$)/i, consumed: true},
        {title: "API", label: /^api usage(?:\s|$)/i, consumed: true}
    ], ["Free", "Pro", "Pro+", "Ultra", "Teams", "Enterprise"]);
};
globalThis.__agentMeterPage ? globalThis.__agentMeterReaders.cursor(globalThis.__agentMeterPage) : undefined;
