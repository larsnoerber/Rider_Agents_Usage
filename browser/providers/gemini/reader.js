globalThis.__agentMeterReaders ||= {};
globalThis.__agentMeterReaders.gemini = async page => {
    if (page.origin !== "https://gemini.google.com") return {error: "unsupported"};
    try {
        // The browser sends its own session cookies. Bootstrap values stay inside this isolated page read.
        const html = await page.request("/usage");
        const bootstrap = name => {
            const match = html.match(new RegExp(`"${name}"\\s*:\\s*"((?:\\\\.|[^"\\\\])*)"`));
            return match ? JSON.parse(`"${match[1]}"`) : null;
        };
        const query = new URLSearchParams({
            rpcids: "jSf9Qc",
            "source-path": "/usage",
            hl: "en",
            rt: "c",
            _reqid: String(Math.floor(Math.random() * 899999) + 100000)
        });
        const build = bootstrap("cfb2h"), session = bootstrap("FdrFJe"), csrf = bootstrap("SNlM0e");
        if (build) query.set("bl", build);
        if (session) query.set("f.sid", session);
        const form = new URLSearchParams({"f.req": '[[["jSf9Qc","[]",null,"generic"]]]'});
        if (csrf) form.set("at", csrf);
        const body = await page.request(`/_/BardChatUi/data/batchexecute?${query}`, {method: "POST", body: form});
        // batchexecute frames may contain a length line and an XSSI prefix. Parse JSON lines only.
        const roots = [];
        for (const line of body.split("\n")) {
            if (!line.trimStart().startsWith("[")) continue;
            try {
                roots.push(JSON.parse(line));
            } catch { /* Unsupported frame, never expose raw data. */
            }
        }
        const findRpc = value => {
            if (!Array.isArray(value)) return null;
            if (value[0] === "wrb.fr" && value[1] === "jSf9Qc") return value;
            for (const child of value) {
                const found = findRpc(child);
                if (found) return found;
            }
            return null;
        };
        const rpc = roots.map(findRpc).find(Boolean);
        const containsDenied = value => value === 7 || value === "7" || (Array.isArray(value) && value.some(containsDenied));
        if (rpc && containsDenied(rpc[5])) return {error: "rejected"};
        if (typeof rpc?.[2] === "string") {
            const metrics = new Map();
            const resetTime = value => {
                if (!Array.isArray(value)) return null;
                if (value.length >= 2 && typeof value[0] === "number" && value[0] >= 1000000000 && value[0] <= 9999999999)
                    return new Date(value[0] * 1000).toISOString();
                for (const child of value) {
                    const reset = resetTime(child);
                    if (reset) return reset;
                }
                return null;
            };
            const visit = value => {
                if (!Array.isArray(value)) return;
                if (value.length >= 4 && typeof value[1] === "number" && value[1] >= 0 && value[1] <= 1 && [1, 2].includes(value[2])) {
                    const metric = {
                        title: value[2] === 1 ? "Session" : "Weekly",
                        percent: value[1] * 100,
                        consumed: true,
                        resetsAt: resetTime(value[3])
                    };
                    // Conflicting values for the same period are ambiguous; never silently pick one model.
                    const existing = metrics.get(value[2]);
                    if (existing === undefined) metrics.set(value[2], metric);
                    else if (!existing || existing.percent !== metric.percent || existing.resetsAt !== metric.resetsAt) metrics.set(value[2], null);
                }
                for (const child of value) visit(child);
            };
            visit(JSON.parse(rpc[2]));
            const quotas = [...metrics.values()].filter(Boolean);
            if (quotas.length) return {quotas, plan: "Gemini Apps"};
        }
    } catch (error) {
        if (["rejected", "rateLimited"].includes(error.message)) return {error: error.message};
    }
    return page.readDom([
        {title: "Session", label: /^(?:5[- ]hour(?:\s+(?:usage|limit))?|5[- ]stunden)(?:\s|$)/i, consumed: true},
        {title: "Weekly", label: /^(?:weekly(?:\s+(?:usage|limit))?|wöchentlich)(?:\s|$)/i, consumed: true}
    ], ["Pro", "Ultra", "Gemini Apps"]);
};
globalThis.__agentMeterPage ? globalThis.__agentMeterReaders.gemini(globalThis.__agentMeterPage) : undefined;
