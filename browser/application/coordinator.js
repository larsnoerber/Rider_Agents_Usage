import {providers, providerForUrl, matchesUsagePage} from "../providers/catalog.js";
import {readInBackground} from "./background-reader.js";
import {readSettings} from "../settings/settings.js";
import {sanitizeSnapshot, restoreSnapshot, numericCache} from "../core/snapshots.js";
import {toolbarPresentation} from "../core/toolbar.js";

let pending = null;
let generation = 0;
let readAgain = false;

export function invalidate() {
    generation++;
}

export async function currentContext() {
    const [tab] = await chrome.tabs.query({active: true, lastFocusedWindow: true});
    return {tab, provider: providerForUrl(tab?.url)};
}

export async function stateForCurrentPage() {
    const {tab} = await currentContext();
    return stateForTab(tab);
}

export async function stateForBar(tab, preferredId) {
    const settings = await readSettings();
    const {readings = {}} = await chrome.storage.session.get("readings");
    const {usageCache = {}} = await chrome.storage.local.get("usageCache");
    const overview = [];
    for (const provider of providers) {
        if (!settings.barProviders.includes(provider.id) || !settings.selected.includes(provider.id) ||
            !await chrome.permissions.contains({origins: [provider.origin]})) continue;
        overview.push({
            providerId: provider.id,
            websiteUrl: new URL(provider.url).origin + "/",
            snapshot: readings[provider.id]?.snapshot || restoreSnapshot(provider.id, usageCache[provider.id])
        });
    }
    const current = overview.find(item => item.providerId === preferredId) ||
        overview.find(item => item.providerId === providerForUrl(tab?.url)?.id) || overview[0];
    return {settings, overview, ...current, context: current ? "ready" : "permission"};
}

export async function stateForTab(tab) {
    const provider = providerForUrl(tab?.url);
    const settings = await readSettings();
    if (!provider) return {settings, providerId: null, snapshot: null, context: "unsupportedSite"};
    const allowed = await chrome.permissions.contains({origins: [provider.origin]});
    const {readings = {}} = await chrome.storage.session.get("readings");
    const {usageCache = {}} = await chrome.storage.local.get("usageCache");
    let snapshot = readings[provider.id]?.snapshot ||
        restoreSnapshot(provider.id, usageCache[provider.id]);
    const context = !settings.selected.includes(provider.id) ? "disabled" : !allowed ? "permission" :
        tab.status === "loading" ? "loading" : "ready";
    if (context === "disabled" || context === "permission") snapshot = null;
    else if (snapshot && context !== "ready") snapshot = {...snapshot, plan: null, isCached: true};
    return {settings, providerId: provider.id, snapshot, context};
}

async function setBadge(provider, snapshot) {
    const settings = await readSettings();
    const presentation = toolbarPresentation(provider, snapshot, settings.warningPercent);
    await chrome.action.setBadgeBackgroundColor({color: "#182231"});
    await chrome.action.setBadgeTextColor({color: presentation.color});
    await chrome.action.setBadgeText({text: presentation.text});
    await chrome.action.setTitle({title: presentation.title});
}

export async function refreshCurrentPage() {
    if (pending) {
        readAgain = true;
        await pending;
        return stateForCurrentPage();
    }
    pending = (async () => {
        do {
            readAgain = false;
            await performRead();
        } while (readAgain);
    })();
    try {
        await pending;
    } finally {
        pending = null;
    }
    return stateForCurrentPage();
}

async function performRead() {
    const {tab: active} = await currentContext();
    const tabs = await chrome.tabs.query({});
    if (!providerForUrl(active?.url)) await setBadge(null, null);
    for (const provider of providers) {
        const priority = tab => (matchesUsagePage(provider, tab.url) ? 100 : 0) +
            (new URL(tab.url).pathname === new URL(provider.url).pathname ? 20 : 0) +
            (tab.id === active?.id ? 10 : 0);
        const candidates = tabs.filter(tab => tab.status !== "loading" && !tab.discarded &&
            providerForUrl(tab.url)?.id === provider.id).sort((a, b) => priority(b) - priority(a));
        await readTab(candidates[0] || null, provider);
    }
}

async function publishBadgeForTab(tab, provider, snapshot) {
    const current = await currentContext();
    if ((tab && current.tab?.id === tab.id) || (provider && current.provider?.id === provider.id))
        await setBadge(provider, snapshot);
}

async function readTab(tab, provider) {
    const ownGeneration = generation;
    const settings = await readSettings();
    const allowed = settings.selected.includes(provider.id) && await chrome.permissions.contains({origins: [provider.origin]});
    if (!allowed) return;
    const state = tab ? await stateForTab(tab) : {context: "ready", snapshot: null};
    if (!provider || state.context !== "ready") {
        await publishBadgeForTab(tab, state.context === "disabled" || state.context === "permission" ? null : provider, state.snapshot);
        return;
    }
    const {backoff = {}, lastAttempts = {}} = await chrome.storage.session.get(["backoff", "lastAttempts"]);
    if (backoff[provider.id] > Date.now() || lastAttempts[provider.id] > Date.now() - 60000) {
        await publishBadgeForTab(tab, provider, state.snapshot);
        return;
    }
    lastAttempts[provider.id] = Date.now();
    await chrome.storage.session.set({lastAttempts});
    let result;
    try {
        if (!tab) result = await readInBackground(provider);
        else {
            const bootstrap = await chrome.scripting.executeScript({
                target: {tabId: tab.id},
                files: ["core/page-reader.js"]
            });
            const documentId = bootstrap[0]?.documentId;
            if (!documentId) throw new Error("read");
            const output = await chrome.scripting.executeScript({
                target: {tabId: tab.id, documentIds: [documentId]},
                files: [`providers/${provider.id}/reader.js`]
            });
            result = output[0]?.result;
        }
    } catch {
        result = {error: "failed", diagnostic: {stage: tab ? "injection" : null}};
    }
    const latestTab = tab ? await chrome.tabs.get(tab.id).catch(() => null) : null;
    const latestSettings = await readSettings();
    if (generation !== ownGeneration || (tab && latestTab?.url !== tab.url) ||
        !latestSettings.selected.includes(provider.id) || !await chrome.permissions.contains({origins: [provider.origin]})) {
        // Release the throttle when a queued read must replace a discarded document.
        const {lastAttempts: attempts = {}} = await chrome.storage.session.get("lastAttempts");
        if (attempts[provider.id] === lastAttempts[provider.id]) delete attempts[provider.id];
        await chrome.storage.session.set({lastAttempts: attempts});
        return;
    }
    let snapshot = sanitizeSnapshot(provider.id, result);
    const {usageCache = {}} = await chrome.storage.local.get("usageCache");
    if (snapshot.quotas.length) {
        usageCache[provider.id] = numericCache(snapshot);
        await chrome.storage.local.set({usageCache});
    } else {
        const cached = restoreSnapshot(provider.id, usageCache[provider.id]);
        if (cached) snapshot = {...cached, notice: snapshot.notice, diagnostic: snapshot.diagnostic};
    }
    if (result?.error === "rateLimited") {
        backoff[provider.id] = Date.now() + 15 * 60 * 1000;
        await chrome.storage.session.set({backoff});
    }
    const {readings = {}} = await chrome.storage.session.get("readings");
    readings[provider.id] = {tabId: tab?.id ?? null, snapshot};
    await chrome.storage.session.set({readings});
    await publishBadgeForTab(tab, provider, snapshot);
}

export async function clearSavedUsage() {
    invalidate();
    readAgain = false;
    if (pending) await pending;
    await chrome.storage.local.remove("usageCache");
    await chrome.storage.session.remove(["readings", "backoff", "lastAttempts"]);
    await setBadge(null, null);
}

export async function updateAlarm() {
    const settings = await readSettings();
    await chrome.alarms.create("usage", {periodInMinutes: settings.refreshSeconds / 60});
}
