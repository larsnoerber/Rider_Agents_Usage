import {pathToFileURL} from 'node:url';

// Run the installed official SDK in its own process. Only connection flags leave it.
console.log = console.info = console.warn = console.error = () => {
};
try {
    const {ProviderSettingsManager, listLocalProviders} = await import(pathToFileURL(process.argv[2]).href);
    const manager = new ProviderSettingsManager();
    const result = await listLocalProviders(manager, {isClinePassEnabled: true});
    const connected = result.providers.some(provider => provider.enabled && provider.configured);
    process.stdout.write(JSON.stringify({connected}));
} catch {
    process.stdout.write(JSON.stringify({connected: null}));
    process.exitCode = 1;
}
