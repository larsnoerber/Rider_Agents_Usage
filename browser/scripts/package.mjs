import {readFile, writeFile, readdir, mkdir, copyFile, rm} from "node:fs/promises";
import {fileURLToPath} from "node:url";
import {dirname, join, resolve, relative} from "node:path";
import {spawnSync} from "node:child_process";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const target = join(root, "dist", "unpacked");
const version = process.argv[2];
if (!/^\d+\.\d+\.\d+(\.\d+)?$/.test(version || "")) throw new Error("Pass the shared project version.");
const manifest = JSON.parse(await readFile(join(root, "manifest.json"), "utf8"));
manifest.version = version;

async function files(directory) {
    const result = [];
    for (const item of await readdir(directory, {withFileTypes: true})) {
        const path = join(directory, item.name);
        if (item.isDirectory()) result.push(...await files(path));
        else result.push(path);
    }
    return result;
}

const sources = [];
for (const directory of ["application", "core", "providers", "settings", "ui", "resources"])
    sources.push(...await files(join(root, directory)));
for (const path of sources.filter(path => path.endsWith(".js"))) {
    const checked = spawnSync(process.execPath, ["--check", path], {encoding: "utf8"});
    if (checked.status !== 0) throw new Error(`Syntax check failed: ${relative(root, path)}\n${checked.stderr}`);
    const code = await readFile(path, "utf8");
    for (const match of code.matchAll(/\bfrom\s+["']([^"']+)["']/g)) {
        if (!match[1].startsWith(".")) throw new Error("Only packaged local modules are allowed.");
        await readFile(resolve(dirname(path), match[1]));
    }
    for (const match of code.matchAll(/\bimport\s+["']([^"']+)["']/g)) {
        if (!match[1].startsWith(".")) throw new Error("Only packaged local modules are allowed.");
        await readFile(resolve(dirname(path), match[1]));
    }
}
for (const path of sources.filter(path => path.endsWith(".html"))) {
    const html = await readFile(path, "utf8");
    for (const match of html.matchAll(/(?:src|href)="([^"]+)"/g)) {
        if (match[1].includes(":")) throw new Error("Remote resources are not allowed in extension pages.");
        await readFile(resolve(dirname(path), match[1]));
    }
}
for (const path of [manifest.background.service_worker, manifest.action.default_popup, manifest.options_ui.page, ...Object.values(manifest.icons)])
    await readFile(join(root, path));
// Resolve and bound the generated directory before recursive deletion.
await readFile(join(root, "ui/page-bar.js"));
if (relative(join(root, "dist"), target) !== "unpacked") throw new Error("Unsafe output directory.");
await rm(target, {recursive: true, force: true});
await mkdir(target, {recursive: true});
for (const path of sources) {
    const destination = join(target, relative(root, path));
    await mkdir(dirname(destination), {recursive: true});
    await copyFile(path, destination);
}
await writeFile(join(target, "manifest.json"), JSON.stringify(manifest, null, 2) + "\n");
for (const path of ["README.md", "PRIVACY.md"]) await copyFile(join(root, path), join(target, path));
await copyFile(join(root, "..", "LICENSE"), join(target, "LICENSE"));
console.log(`Packaged ${sources.length} source/assets files · version ${version}. JavaScript syntax and local references checked.`);
