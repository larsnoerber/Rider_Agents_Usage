import "../core/page-reader.js";
import "../providers/codex/reader.js";
import "../providers/claude/reader.js";
import "../providers/cursor/reader.js";
import "../providers/copilot/reader.js";
import "../providers/gemini/reader.js";
import {providerById} from "../providers/catalog.js";
import {sanitizeSnapshot} from "../core/snapshots.js";

const helpers = globalThis.__agentMeterPage;
chrome.runtime.onMessage.addListener((message, sender, reply) => {
    if (sender.id !== chrome.runtime.id || sender.tab || message?.target !== "quotaReader") return false;
    const provider = providerById(message.providerId);
    if (!provider) return false;
    const origin = new URL(provider.origin).origin;
    const page = {
        origin, pathname: "/",
        request: (path, options) => helpers.request(path, options, origin),
        readDom: (...args) => helpers.readDom(...args),
        parseHtml: helpers.parseHtml,
        selectedOrganization: () => null
    };
    void globalThis.__agentMeterReaders[provider.id](page).then(result => {
        // Never pass raw responses, identifiers or credentials across the runtime boundary.
        const snapshot = sanitizeSnapshot(provider.id, result);
        reply({
            quotas: snapshot.quotas, plan: snapshot.plan,
            diagnostic: snapshot.diagnostic,
            error: Object.hasOwn(allowedErrors, result?.error) ? result.error : "unsupported"
        });
    }, () => reply({error: "failed"}));
    return true;
});

// Fixed safe error identifiers only.
const allowedErrors = {
    unsupported: true, noLabels: true, noPercentages: true, noDirection: true,
    rejected: true, failed: true, rateLimited: true, organizationRequired: true,
    noQuota: true, claudeNoQuota: true, quotaFormat: true
};
