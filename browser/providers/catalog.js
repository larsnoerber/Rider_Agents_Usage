export const providers = Object.freeze([
    {
        id: "codex",
        name: "OpenAI Codex",
        label: "OpenAi",
        origin: "https://chatgpt.com/*",
        url: "https://chatgpt.com/codex/settings/usage",
        paths: ["/codex/settings/usage", "/codex/cloud/settings/usage", "/codex/cloud/settings/analytics"],
        color: "#67dab1"
    },
    {
        id: "claude", name: "Claude", label: "Claude", origin: "https://claude.ai/*",
        url: "https://claude.ai/settings/usage", paths: ["/settings/usage"], color: "#f3b38d"
    },
    {
        id: "cursor", name: "Cursor", label: "Cursor", origin: "https://cursor.com/*",
        url: "https://cursor.com/dashboard?tab=usage", paths: ["/dashboard"], color: "#d1e2f5"
    },
    {
        id: "copilot",
        name: "GitHub",
        label: "GitHub",
        origin: "https://github.com/*",
        url: "https://github.com/settings/copilot/features",
        paths: ["/settings/copilot", "/settings/copilot/features", "/settings/billing", "/settings/billing/usage", "/settings/billing/premium_requests", "/settings/billing/summary"],
        color: "#84b8ff"
    },
    {
        id: "gemini", name: "Gemini Apps", label: "Gemini", origin: "https://gemini.google.com/*",
        url: "https://gemini.google.com/usage", paths: ["/usage"], color: "#a4b8ff"
    }
]);
export const providerById = id => providers.find(provider => provider.id === id);

export function providerForUrl(value) {
    try {
        return providers.find(provider => new URL(provider.url).origin === new URL(value).origin);
    } catch {
        return undefined;
    }
}

export function matchesUsagePage(provider, value) {
    try {
        const url = new URL(value);
        return url.origin === new URL(provider.url).origin && provider.paths.some(path =>
            url.pathname === path || url.pathname === path + "/");
    } catch {
        return false;
    }
}
