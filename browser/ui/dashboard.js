import {providers} from "../providers/catalog.js";
import {readSettings} from "../settings/settings.js";

const $ = id => document.getElementById(id);
const settings = await readSettings();
document.body.dataset.theme = settings.theme;
const fields = {
    style: ["style", "value"], theme: ["theme", "value"],
    interval: ["refreshSeconds", "value"], warning: ["warningPercent", "value"],
    "page-bar": ["showPageBar", "checked"], "all-sites-bar": ["allSitesBar", "checked"],
    "bar-position": ["pageBarPosition", "value"]
};
for (const [id, [key, property]] of Object.entries(fields)) $(id)[property] = settings[key];

function status(text, failed = false) {
    $("status").textContent = text;
    $("status").dataset.failed = String(failed);
}

let saves = Promise.resolve();
let revision = 0;

function save(patch) {
    const ownRevision = ++revision;
    status("Saving changes…");
    const saving = saves.catch(() => {
    }).then(async () => {
        const response = await chrome.runtime.sendMessage({type: "settingsPatch", patch});
        if (!response?.settings || response.error) throw new Error("save");
        document.body.dataset.theme = response.settings.theme;
    });
    saves = saving;
    void saving.then(() => {
        if (revision === ownRevision) status("All changes saved.");
    }, () => status("Changes could not be saved. Change the setting again to retry.", true));
    return saving;
}

for (const [id, [key, property]] of Object.entries(fields)) {
    $(id).addEventListener("change", () => {
        if (!$(id).checkValidity()) return $(id).reportValidity();
        const value = $(id)[property];
        const patch = {[key]: ["refreshSeconds", "warningPercent"].includes(key) ? Number(value) : value};
        if (id === "bar-position") patch.barAnchor = null;
        void save(patch).catch(() => {
        });
    });
}
$("reset-position").addEventListener("click", () => void save({barAnchor: null}).catch(() => {
}));
$("providers").addEventListener("change", () => {
    const selected = [...$("providers").querySelectorAll("input:checked")].map(input => input.value);
    void save({selected}).catch(() => {
    });
});

const accessButtons = [];

async function updateAccess() {
    for (const {provider, button} of accessButtons) {
        const allowed = await chrome.permissions.contains({origins: [provider.origin]});
        button.dataset.allowed = String(allowed);
        button.textContent = allowed ? "Remove access" : "Allow access";
    }
}

for (const provider of providers) {
    const row = document.createElement("div");
    row.className = "provider-setting";
    const label = document.createElement("label");
    const checkbox = document.createElement("input");
    checkbox.type = "checkbox";
    checkbox.value = provider.id;
    checkbox.checked = settings.selected.includes(provider.id);
    label.append(checkbox, document.createTextNode(provider.name));
    const button = document.createElement("button");
    button.textContent = "Checking access…";
    button.disabled = true;
    accessButtons.push({provider, button});
    button.addEventListener("click", () => {
        const remove = button.dataset.allowed === "true";
        const operation = remove ? chrome.permissions.remove({origins: [provider.origin]}) :
            chrome.permissions.request({origins: [provider.origin]});
        button.disabled = true;
        void operation.then(async success => {
            await updateAccess();
            const allowed = await chrome.permissions.contains({origins: [provider.origin]});
            status(remove && allowed ? "Access is still granted by the all-websites permission. Manage it in your browser's extension settings." :
                success ? `${provider.name} access ${remove ? "removed" : "granted"}.` : "Website access was not changed.");
        }).catch(() => status("Could not change website access.", true)).finally(() => {
            button.disabled = false;
        });
    });
    const link = document.createElement("a");
    link.href = provider.url;
    link.target = "_blank";
    link.rel = "noopener noreferrer";
    link.textContent = "Usage page ↗";
    row.append(label, button, link);
    $("providers").append(row);
}
await updateAccess();
for (const {button} of accessButtons) button.disabled = false;
$("allow-all").addEventListener("click", () => {
    const granting = chrome.permissions.request({origins: ["https://*/*", "http://*/*"]});
    $("allow-all").disabled = true;
    void granting.then(async granted => {
        if (!granted) return status("Website access was not granted.");
        $("page-bar").checked = true;
        $("all-sites-bar").checked = true;
        await save({showPageBar: true, allSitesBar: true});
        await updateAccess();
    }).catch(() => status("Could not enable website access.", true)).finally(() => {
        $("allow-all").disabled = false;
    });
});
$("clear").addEventListener("click", async () => {
    $("clear").disabled = true;
    try {
        const response = await chrome.runtime.sendMessage({type: "clear"});
        if (response?.error) throw new Error("clear");
        status("Saved usage cleared.");
    } catch {
        status("Could not clear saved usage.", true);
    } finally {
        $("clear").disabled = false;
    }
});
