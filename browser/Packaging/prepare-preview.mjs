// Store presentation fixtures only. Never included in the extension ZIP.
import {readFile, writeFile, mkdir, cp} from "node:fs/promises";
import {dirname, resolve, join} from "node:path";
import {fileURLToPath} from "node:url";
const browserRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const preview = join(browserRoot, "dist", "store-preview");
await mkdir(preview, {recursive: true});
for (const directory of ["ui", "providers", "settings", "core", "resources"])
    await cp(join(browserRoot, directory), join(preview, directory), {recursive: true});

const demo = {
    selected: ["codex", "copilot", "gemini"], barProviders: ["codex", "copilot", "gemini"],
    showPageBar: true, allSitesBar: true, pageBarPosition: "bottom", barAnchor: null,
    warningPercent: 25, refreshSeconds: 300, style: "cards", theme: "dark"
};
const measured = new Date().toISOString();
const quota = (title, percent, consumed) => ({title, percent, consumed,
    resetsAt: new Date(Date.now() + 8100000).toISOString()});
const overview = [
    {providerId: "codex", websiteUrl: "https://chatgpt.com/", snapshot: {plan: "Plus", updatedAt: measured,
        quotas: [quota("Session", 59, false), quota("Weekly", 79, false)], isCached: false}},
    {providerId: "copilot", websiteUrl: "https://github.com/", snapshot: {plan: "Free", updatedAt: measured,
        quotas: [quota("Included credits", 4, true), quota("Inline suggestions", 0, true)], isCached: false}},
    {providerId: "gemini", websiteUrl: "https://gemini.google.com/", snapshot: {updatedAt: measured,
        quotas: [quota("Session", 0, true), quota("Weekly", 1, true)], isCached: false}}
];
const mock = `<script>
const demoSettings = ${JSON.stringify(demo)};
const demoOverview = ${JSON.stringify(overview)};
globalThis.chrome = {
  storage: {local: {get: async () => ({settings: demoSettings})}},
  permissions: {contains: async () => true, request: async () => true, remove: async () => false},
  runtime: {id: "agentmeter-store-demo", onMessage: {addListener() {}, removeListener() {}},
    sendMessage: async message => {
      if (message.type === "settingsPatch") return {settings: Object.assign(demoSettings, message.patch)};
      if (message.type === "pageBarPosition") return {ok: true};
      return {settings: demoSettings, overview: demoOverview, ...demoOverview[0], context: "ready"};
    }}
};
</script>`;
const settingsHtml = await readFile(join(preview, "ui/dashboard.html"), "utf8");
await writeFile(join(preview, "ui/dashboard.html"), settingsHtml.replace("</head>", `${mock}</head>`)
    .replace("<main>", '<p style="color:#a4b2c6;font-size:12px">Store preview · demonstration permissions and provider selection</p><main>'));
await writeFile(join(preview, "bar.html"), `<!doctype html><html lang="en"><head><meta charset="utf-8">
<title>AgentMeter store preview</title>${mock}<style>
*{box-sizing:border-box}body{margin:0;background:#0e1520;color:#eef3fb;font:18px/1.6 "Segoe UI",sans-serif}
main{max-width:1100px;margin:64px auto}header{display:flex;align-items:center;gap:20px;margin-bottom:50px}
header img{width:70px;height:70px}h1{font-size:44px;line-height:1.2;margin:0}h2{font-size:29px;margin:0 0 14px}
p{color:#a4b2c6;margin:10px 0}.demo{color:#a4b8ff;font-size:14px}.card{background:#182231;border:1px solid #2b3a4e;
border-radius:18px;padding:34px}.grid{display:grid;grid-template-columns:1fr 1fr;gap:24px;margin-top:26px}
.small{padding:24px}strong{color:#67dab1}.tag{display:inline-block;font-size:14px;padding:6px 12px;border-radius:20px;
background:#25344a;color:#a4b8ff}footer{font-size:14px;margin-top:24px;color:#a4b2c6}
</style></head><body><main><header><img src="resources/icon.png" alt=""><div><h1>AgentMeter</h1>
<p>Keep your AI quotas in view.</p></div></header><section class="card"><span class="tag">Chrome &amp; Edge</span>
<h2 style="margin-top:18px">One bar for your selected providers</h2><p>View reported quotas while you browse.
Open provider websites with a click.</p><p class="demo">Store preview · demonstration quota values, not live account data</p></section>
<div class="grid"><section class="card small"><h2>Move it where you need it</h2><p>Drag the handle. Your position is remembered.</p></section>
<section class="card small"><h2>Know when quotas reset</h2><p>Expand details for countdowns and last-update times.</p></section></div>
<footer>Optional website access · configurable quota colors · local settings and cache</footer></main>
<script src="core/quota-display.js"></script><script src="ui/bar-position.js"></script><script src="ui/page-bar.js"></script>
</body></html>`);
console.log(preview);
