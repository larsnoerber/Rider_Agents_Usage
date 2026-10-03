import {readFile, writeFile} from "node:fs/promises";
import {spawnSync} from "node:child_process";
import {fileURLToPath} from "node:url";
import {dirname, resolve} from "node:path";

const extensionRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const repositoryRoot = resolve(extensionRoot, "..");
const gradleProperties = await readFile(resolve(repositoryRoot, "gradle.properties"), "utf8");
const versionLine = gradleProperties.split(/\r?\n/).find((line) => line.startsWith("pluginVersion="));
if (!versionLine) throw new Error("pluginVersion was not found in the repository gradle.properties");

const version = versionLine.slice("pluginVersion=".length).trim();
if (!/^\d+\.\d+\.\d+(?:[-+][0-9A-Za-z.-]+)?$/.test(version)) {
    throw new Error(`Invalid shared extension version: ${version}`);
}

const manifestPath = resolve(extensionRoot, "package.json");
const manifest = JSON.parse(await readFile(manifestPath, "utf8"));
manifest.version = version;
await writeFile(manifestPath, `${JSON.stringify(manifest, null, 2)}\n`, "utf8");

const vsce = resolve(extensionRoot, "node_modules", "@vscode", "vsce", "vsce");
const repository = "https://github.com/larsnoerber/Rider_Agents_Usage";
const result = spawnSync(process.execPath, [vsce, "package", "--out", `agents-usage-vscode-${version}.vsix`,
    "--baseContentUrl", `${repository}/blob/v${version}/vscode/`,
    "--baseImagesUrl", `https://raw.githubusercontent.com/larsnoerber/Rider_Agents_Usage/v${version}/vscode/`], {
    cwd: extensionRoot,
    stdio: "inherit",
});
if (result.error) throw result.error;
if (result.status !== 0) process.exit(result.status ?? 1);
