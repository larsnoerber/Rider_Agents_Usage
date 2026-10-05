// Shared formatting for module UIs and the isolated classic page bar.
(() => {
    const remaining = quota => quota.consumed ? 100 - quota.percent : quota.percent;
    const level = (quota, warning = 25) => {
        const left = remaining(quota);
        return left <= Math.min(10, warning) ? "danger" : left <= warning ? "warning" : "healthy";
    };
    const age = value => {
        const elapsed = Date.now() - Date.parse(value);
        if (!Number.isFinite(elapsed)) return "Not read yet";
        const minutes = Math.max(0, Math.floor(elapsed / 60000));
        return minutes === 0 ? "Just now" : minutes < 60 ? `${minutes} min ago` : `${Math.floor(minutes / 60)} h ago`;
    };
    const reset = value => {
        const elapsed = Date.parse(value) - Date.now();
        if (!Number.isFinite(elapsed)) return "";
        const minutes = Math.ceil(elapsed / 60000);
        if (minutes <= 0) return "Reset due · refresh to check";
        if (minutes < 60) return `Resets in ${minutes} min`;
        if (minutes < 1440) return `Resets in ${Math.floor(minutes / 60)} h ${minutes % 60} min`;
        return `Resets in ${Math.floor(minutes / 1440)} d ${Math.floor(minutes % 1440 / 60)} h`;
    };
    const observation = snapshot => snapshot?.updatedAt && Number.isFinite(Date.parse(snapshot.updatedAt)) ?
        `${snapshot.isCached ? "Saved value" : "Last updated"} · ${age(snapshot.updatedAt)} · ${new Date(snapshot.updatedAt).toLocaleString()}` :
        "No measured quota yet";
    globalThis.__agentMeterDisplay = Object.freeze({remaining, level, age, reset, observation});
})();
