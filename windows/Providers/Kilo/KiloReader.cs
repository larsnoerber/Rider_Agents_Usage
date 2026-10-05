using System.Globalization;
using System.IO;
using System.Text.Json;
using System.Text.RegularExpressions;
using AgentMeter.Windows.Core;
using AgentMeter.Windows.Core.Processes;

namespace AgentMeter.Windows.Providers.Kilo;

internal sealed class KiloReader : IUsageReader
{
    private readonly CancellationTokenSource lifetime = new();
    public string Id => "kilo";

    public async Task<UsageSnapshot> ReadAsync(string executablePath, CancellationToken cancellationToken)
    {
        using var linked = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken, lifetime.Token);
        var path = AgentExecutables.Find(Id, executablePath);
        if (path == null) return UsageSnapshot.Unavailable(Id, "Kilo CLI not found. Install Kilo or select its executable in Settings.");
        var quotas = new List<UsageQuota>();
        bool? signedIn = null;
        string? notice = null;
        try
        {
            // Saved provider connections (including BYOK) are separate from Gateway balance availability.
            var auth = await ReadOnlyAgentCommand.RunAsync(path, "--pure auth list", linked.Token);
            if (auth.ExitCode == 0)
            {
                signedIn = AgentStatistics.Connected(auth.Output + "\n" + auth.Error);
            }
            var profile = await ReadOnlyAgentCommand.RunAsync(path, "--pure profile --json", linked.Token);
            if (profile.ExitCode == 0 && string.IsNullOrWhiteSpace(profile.Error))
            {
                using var json = JsonDocument.Parse(profile.Output);
                if (json.RootElement.TryGetProperty("balance", out var value) && value.TryGetDouble(out var balance)
                    && double.IsFinite(balance))
                {
                    quotas.Add(new("Credit balance", null, false, DisplayValue: balance.ToString("$0.00", CultureInfo.InvariantCulture)));
                    signedIn = true;
                }
            }
            else
            {
                var gatewayUnavailable = profile.Error.Contains("Not authenticated with Kilo Gateway", StringComparison.OrdinalIgnoreCase);
                if (gatewayUnavailable && signedIn != true) signedIn = false;
                // CLI can substitute zero for a failed balance request. Never publish that value.
                notice = gatewayUnavailable && signedIn == true ? "Connected to a model provider. No Kilo Gateway balance applies to this connection."
                    : signedIn == false ? "Not connected to Kilo Gateway. Local usage can still be available."
                    : "Kilo balance is temporarily unavailable; local usage is shown when available.";
            }
            var stats = await ReadOnlyAgentCommand.RunAsync(path, "--pure stats --days 30", linked.Token);
            if (stats.ExitCode == 0)
            {
                var text = Regex.Replace(stats.Output, @"\x1B\[[0-?]*[ -/]*[@-~]", "");
                AgentStatistics.AddReported(quotas, text, "Total Cost", "Cost · last 30 days", @"\$[0-9]+(?:\.[0-9]+)?");
                AgentStatistics.AddReported(quotas, text, "Input", "Input tokens · last 30 days", @"[0-9]+(?:\.[0-9]+)?[KMB]?");
                AgentStatistics.AddReported(quotas, text, "Output", "Output tokens · last 30 days", @"[0-9]+(?:\.[0-9]+)?[KMB]?");
            }
            return new(Id, quotas.Any(q => q.Title == "Credit balance") ? "Kilo Gateway" : "Local usage", quotas,
                notice ?? (quotas.Count == 0 ? "No Kilo usage reported yet. Use Kilo, then refresh." : null),
                ["Costs and tokens cover local Kilo sessions in the last 30 days, including other model providers.",
                 "Credit balance belongs to the active Kilo account or team. It is separate from local session costs."],
                DateTimeOffset.Now) { SignedIn = signedIn };
        }
        catch (OperationCanceledException) when (!cancellationToken.IsCancellationRequested && !lifetime.IsCancellationRequested)
        { return new(Id, null, quotas, "Kilo did not respond in time. AgentMeter retries automatically.") { SignedIn = signedIn }; }
        catch (Exception error) when (error is IOException or JsonException or InvalidOperationException or System.ComponentModel.Win32Exception)
        { return new(Id, null, quotas, "Kilo usage is unavailable. Check the CLI connection and refresh.") { SignedIn = signedIn }; }
    }

    public void Dispose() { lifetime.Cancel(); lifetime.Dispose(); }
}
