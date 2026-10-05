using System.IO;
using System.Text.Json;
using AgentMeter.Windows.Core;
using AgentMeter.Windows.Core.Processes;

namespace AgentMeter.Windows.Providers.OpenCode;

internal sealed class OpenCodeReader : IUsageReader
{
    private readonly CancellationTokenSource lifetime = new();
    public string Id => "opencode";

    public async Task<UsageSnapshot> ReadAsync(string executablePath, CancellationToken cancellationToken)
    {
        using var linked = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken, lifetime.Token);
        var path = AgentExecutables.Find(Id, executablePath);
        if (path == null) return UsageSnapshot.Unavailable(Id, "OpenCode CLI not found. Install OpenCode or select its executable in Settings.");
        bool? connected = null;
        var quotas = new List<UsageQuota>();
        try
        {
            var version = await ReadOnlyAgentCommand.RunAsync(path, "--version", linked.Token);
            var jsonFormat = OpenCodeStatistics.UsesJson(version.Output);
            // auth list reports connection metadata; auth export would expose credentials and must never be used here.
            var auth = await ReadOnlyAgentCommand.RunAsync(path, jsonFormat ? "auth list --format json" : "auth list", linked.Token);
            if (auth.ExitCode == 0)
                connected = OpenCodeStatistics.Connected(jsonFormat ? auth.Output : auth.Output + "\n" + auth.Error, jsonFormat);
            var stats = await ReadOnlyAgentCommand.RunAsync(path, jsonFormat ? "stats --days 30 --json" : "stats --days 30 --tools 0", linked.Token);
            if (stats.ExitCode == 0) OpenCodeStatistics.AddUsage(quotas, stats.Output, jsonFormat);
            return new(Id, "Local usage", quotas,
                quotas.Count == 0 ? "No OpenCode usage reported. Connect a provider and use OpenCode, then refresh." : null,
                ["Reported local statistics cover all OpenCode projects over the last 30 days.",
                 "Connection means OpenCode reports a configured credential. Costs are local estimates; remaining provider credits are separate."],
                DateTimeOffset.Now)
            { SignedIn = connected };
        }
        catch (OperationCanceledException) when (!cancellationToken.IsCancellationRequested && !lifetime.IsCancellationRequested)
        { return new(Id, "Local usage", quotas, "OpenCode did not respond in time. AgentMeter retries automatically.") { SignedIn = connected }; }
        catch (Exception e) when (e is IOException or JsonException or InvalidOperationException or System.ComponentModel.Win32Exception)
        { return new(Id, "Local usage", quotas, "OpenCode usage is temporarily unavailable.") { SignedIn = connected }; }
    }

    public void Dispose() { lifetime.Cancel(); lifetime.Dispose(); }
}
