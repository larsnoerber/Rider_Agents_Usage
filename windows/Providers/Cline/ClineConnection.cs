using System.IO;
using System.Text.Json;
using AgentMeter.Windows.Core.Processes;

namespace AgentMeter.Windows.Providers.Cline;

internal static class ClineConnection
{
    internal static async Task<bool?> ReadAsync(string configuredPath, CancellationToken token)
    {
        var cli = AgentExecutables.Find("cline", configuredPath);
        var node = AgentExecutables.Find("node");
        var helper = Path.Combine(AppContext.BaseDirectory, "Providers", "Cline", "auth-status.mjs");
        if (cli == null || node == null || !File.Exists(helper)) return null;
        var directory = Path.GetDirectoryName(cli);
        string? sdk = null;
        for (var i = 0; directory != null && i < 7; i++, directory = Path.GetDirectoryName(directory))
        {
            foreach (var candidate in new[] { Path.Combine(directory, "@cline", "core", "dist", "index.js"),
                         Path.Combine(directory, "node_modules", "@cline", "core", "dist", "index.js") })
                if (File.Exists(candidate)) { sdk = candidate; break; }
            if (sdk != null) break;
        }
        if (sdk == null) return null;
        try
        {
            var response = await ReadOnlyAgentCommand.RunAsync(node, $"\"{helper}\" \"{sdk}\"", token);
            if (response.ExitCode != 0) return null;
            using var json = JsonDocument.Parse(response.Output);
            return json.RootElement.TryGetProperty("connected", out var state) && state.ValueKind is JsonValueKind.True or JsonValueKind.False
                ? state.GetBoolean() : null;
        }
        catch (Exception) when (!token.IsCancellationRequested) { return null; }
    }
}
