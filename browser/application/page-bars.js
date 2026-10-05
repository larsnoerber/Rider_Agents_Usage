import {providers, providerForUrl} from "../providers/catalog.js";
import {readSettings} from "../settings/settings.js";
import {stateForBar} from "./coordinator.js";

const prefix = "agentmeter-page-bar-";
const allOrigins = ["https://*/*", "http://*/*"];
let synchronization = Promise.resolve();

export async function removePageBars() {
    const tabs = await chrome.tabs.query({});
    await Promise.allSettled(tabs.map(tab => chrome.tabs.sendMessage(tab.id, {type: "pageBarRemove"})));
}

export function syncPageBars() {
    synchronization = synchronization.catch(() => {
    }).then(synchronize);
    return synchronization;
}

async function displayPatterns(settings) {
    if (!settings.showPageBar) return [];
    if (settings.allSitesBar && await chrome.permissions.contains({origins: allOrigins})) return allOrigins;
    const patterns = [];
    for (const provider of providers) {
        if (settings.selected.includes(provider.id) && await chrome.permissions.contains({origins: [provider.origin]})) patterns.push(provider.origin);
    }
    return patterns;
}

async function synchronize() {
    const patterns = await displayPatterns(await readSettings());
    const registered = (await chrome.scripting.getRegisteredContentScripts()).filter(script => script.id.startsWith(prefix));
    if (registered.length) await chrome.scripting.unregisterContentScripts({ids: registered.map(script => script.id)});
    await removePageBars();
    if (patterns.length) {
        await chrome.scripting.registerContentScripts([{
            id: prefix + "display", matches: patterns, js: ["core/quota-display.js", "ui/bar-position.js", "ui/page-bar.js"], runAt: "document_idle",
            allFrames: false, persistAcrossSessions: true, world: "ISOLATED"
        }]);
        const tabs = await chrome.tabs.query({url: patterns});
        await Promise.allSettled(tabs.map(tab => chrome.scripting.executeScript({
            target: {tabId: tab.id},
            files: ["core/quota-display.js", "ui/bar-position.js", "ui/page-bar.js"]
        })));
    }
    await publishPageBars();
}

export async function publishPageBars() {
    const patterns = await displayPatterns(await readSettings());
    if (!patterns.length) return;
    const tabs = await chrome.tabs.query({url: patterns});
    await Promise.allSettled(tabs.map(async tab => chrome.tabs.sendMessage(tab.id, {
        type: "pageBarState", state: await stateForBar(tab)
    })));
}

export async function isProviderPageSender(sender) {
    if (sender.id !== chrome.runtime.id || sender.frameId !== 0 || !sender.tab?.id) return false;
    const settings = await readSettings();
    if (!settings.showPageBar) return false;
    let url;
    try {
        url = new URL(sender.url);
        if (!["http:", "https:"].includes(url.protocol) || new URL(sender.tab.url).origin !== url.origin) return false;
    } catch {
        return false;
    }
    if (settings.allSitesBar && await chrome.permissions.contains({origins: allOrigins})) return true;
    const provider = providerForUrl(sender.url);
    return Boolean(provider && settings.selected.includes(provider.id) && await chrome.permissions.contains({origins: [provider.origin]}));
}
