import {ageText, percentText} from "./format.js";

// Chromium offers a small badge and a hover title, not arbitrary text beside the address bar.
export function toolbarPresentation(provider, snapshot, warning = 25) {
    const quota = snapshot?.quotas?.[0];
    if (!provider) return {text: "", title: "AgentMeter · Open a provider usage page", color: "#a4b2c6"};
    const value = title => {
        const item = snapshot?.quotas?.find(candidate => candidate.title === title);
        return item ? percentText(item.percent) : "—";
    };
    const summary = provider.id === "codex" ? `${provider.label} | D=${value("Session")} - W=${value("Weekly")}` :
        provider.id === "gemini" ? `${provider.label} | 5h=${value("Session")} - W=${value("Weekly")}` :
            `${provider.label} | ${quota ? percentText(quota.percent) : "—"}`;
    const left = quota ? quota.consumed ? 100 - quota.percent : quota.percent : null;
    const color = snapshot?.isCached ? "#a4b2c6" : left === null ? "#a4b2c6" :
        left <= Math.min(10, warning) ? "#ff8396" : left <= warning ? "#f4cd6b" : "#67dab1";
    const meaning = quota ? `${quota.consumed ? "Consumed" : "Remaining"} quota` : "Open the usage page to read quota";
    const freshness = snapshot?.updatedAt ? `\n${snapshot.isCached ? "Saved value" : "Read"} · ${ageText(snapshot.updatedAt)} · ${new Date(snapshot.updatedAt).toLocaleString()}` : "";
    return {text: quota ? percentText(quota.percent) : "—", title: `${summary}\n${meaning}${freshness}`, color};
}
