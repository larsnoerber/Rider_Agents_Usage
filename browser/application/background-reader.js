let creating = null;

export async function readInBackground(provider) {
    const path = "application/offscreen.html";
    const contexts = await chrome.runtime.getContexts({
        contextTypes: ["OFFSCREEN_DOCUMENT"], documentUrls: [chrome.runtime.getURL(path)]
    });
    if (!contexts.length) {
        creating ||= chrome.offscreen.createDocument({
            url: path, reasons: ["DOM_PARSER"],
            justification: "Parse fixed provider usage pages for the user-authorized background quota display."
        });
        try {
            await creating;
        } finally {
            creating = null;
        }
    }
    return chrome.runtime.sendMessage({target: "quotaReader", providerId: provider.id});
}
