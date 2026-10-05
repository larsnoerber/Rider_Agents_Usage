using System.IO;
using System.Text.Json.Nodes;
using AgentMeter.Windows.Core;

namespace AgentMeter.Windows.Providers.Claude;

internal sealed class ClaudeReader : IUsageReader
{
    private readonly ProviderHttp http = new(["api.anthropic.com"]);
    public string Id => "claude";

    public async Task<UsageSnapshot> ReadAsync(string executablePath, CancellationToken cancellationToken)
    {
        var configured = Environment.GetEnvironmentVariable("CLAUDE_CONFIG_DIR");
        var root = !string.IsNullOrWhiteSpace(configured) && Path.IsPathFullyQualified(configured)
            ? configured : Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), ".claude");
        var file = Path.Combine(root, ".credentials.json");
        if (!File.Exists(file)) return await ClaudeAuthStatus.ReadAsync(executablePath, cancellationToken);
        try
        {
            string? accessToken;
            string? plan;
            // Parse only the official provider store; credentials never enter app settings or snapshots.
            {
                var auth = JsonNode.Parse(await File.ReadAllTextAsync(file, cancellationToken)).Object("claudeAiOauth");
                accessToken = auth.Text("accessToken");
                plan = auth.Text("subscriptionType");
                if (auth.Number("expiresAt") is double expires && expires <= DateTimeOffset.UtcNow.ToUnixTimeMilliseconds())
                    return UsageSnapshot.Unavailable(Id, "Claude session expired. Open Claude Code to renew your sign-in.") with { SignedIn = false };
            }
            if (accessToken == null) return await ClaudeAuthStatus.ReadAsync(executablePath, cancellationToken);
            JsonObject response;
            try
            {
                response = await http.SendAsync("https://api.anthropic.com/api/oauth/usage", new Dictionary<string, string>
                {
                    ["Authorization"] = "Bearer " + accessToken,
                    ["anthropic-beta"] = "oauth-2025-04-20", ["User-Agent"] = "claude-code/2.1.131"
                }, cancellationToken);
            }
            finally { accessToken = null; }
            var quotas = new List<UsageQuota>();
            foreach (var (key, title) in new[] { ("five_hour", "Session remaining"), ("seven_day", "Weekly remaining"),
                         ("seven_day_sonnet", "Sonnet remaining"), ("seven_day_opus", "Opus remaining") })
            {
                var window = response.Object(key);
                if (window.Number("utilization") is double used)
                    quotas.Add(new(title, 100 - JsonFields.Clamp(used), false, JsonFields.Date(window.Text("resets_at"))));
            }
            return new(Id, plan, quotas, quotas.Count == 0 ? "Claude did not report subscription quotas." : null,
                UpdatedAt: DateTimeOffset.Now) { SignedIn = true };
        }
        catch (ProviderHttpFailure failure) { return UsageSnapshot.Unavailable(Id, failure.SafeMessage("Claude")) with
            { SignedIn = failure.Status is System.Net.HttpStatusCode.Unauthorized or System.Net.HttpStatusCode.Forbidden ? false : null }; }
    }
    public void Dispose() => http.Dispose();
}
