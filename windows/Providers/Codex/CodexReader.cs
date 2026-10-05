using AgentMeter.Windows.Core;
using AgentsUsage.VisualStudio.Providers.Codex;

namespace AgentMeter.Windows.Providers.Codex;

internal sealed class CodexReader : IUsageReader
{
    private readonly CodexUsageReader reader = new();
    public string Id => "codex";
    public async Task<UsageSnapshot> ReadAsync(string executablePath, CancellationToken cancellationToken)
    {
        var usage = await reader.ReadAsync(executablePath, cancellationToken);
        var quotas = new List<UsageQuota>();
        if (usage.FiveHourLeft.HasValue) quotas.Add(new("5-hour remaining", usage.FiveHourLeft, false, usage.FiveHourReset));
        if (usage.WeeklyLeft.HasValue) quotas.Add(new("Weekly remaining", usage.WeeklyLeft, false, usage.WeeklyReset));
        var details = usage.Credits.HasValue ? new[] { $"Credits: {usage.Credits.Value:0.##}" } : [];
        return new(Id, usage.Plan, quotas, usage.Error, details, usage.Error == null ? usage.UpdatedAt : null)
            { SignedIn = usage.SignedIn };
    }
    public void Dispose() => reader.Dispose();
}
