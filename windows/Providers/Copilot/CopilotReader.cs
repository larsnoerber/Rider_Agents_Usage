using System.IO;
using AgentMeter.Windows.Core;
using AgentMeter.Windows.Core.Processes;

namespace AgentMeter.Windows.Providers.Copilot;

internal sealed class CopilotReader : IUsageReader
{
    public string Id => "copilot";
    public async Task<UsageSnapshot> ReadAsync(string executablePath, CancellationToken cancellationToken)
    {
        var path = AgentExecutables.Find(Id, executablePath);
        if (path == null) return UsageSnapshot.Unavailable(Id,
            "Official Copilot Language Server not found. Install it or choose its executable in Settings, then sign in.") with { SignedIn = false };
        using var deadline = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        deadline.CancelAfter(TimeSpan.FromSeconds(30));
        try
        {
            using var connection = new CopilotConnection(path);
            using var registration = deadline.Token.Register(connection.Dispose);
            await connection.InitializeAsync(deadline.Token);
            var response = await connection.RequestAsync("checkQuota", new(), deadline.Token);
            var plan = response.Text("copilotPlan");
            var free = string.Equals(plan, "free", StringComparison.OrdinalIgnoreCase);
            var credits = response.Flag("tokenBasedBillingEnabled");
            var reset = JsonFields.Date(response.Text("resetDateUtc") ?? response.Text("resetDate"));
            var quotas = new List<UsageQuota>();
            foreach (var (key, title) in free
                         ? new[] { ("chat", credits ? "AI credits used" : "Chat used"), ("completions", "Completions used") }
                         : new[] { ("premiumInteractions", credits ? "AI credits used" : "Premium requests used"), ("chat", "Chat used"), ("completions", "Completions used") })
            {
                var quota = response.Object(key);
                if (quota == null) continue;
                var unlimited = quota.Flag("unlimited");
                var remaining = quota.Number("quotaRemaining");
                var entitlement = quota.Number("entitlement");
                if (!unlimited && entitlement == 0 && (remaining == null || remaining == 0)) continue;
                var percentLeft = quota.Number("percentRemaining");
                if (!unlimited && percentLeft == null) continue;
                var detail = !credits && !unlimited && remaining.HasValue && entitlement.HasValue
                    ? $"{Math.Max(0, entitlement.Value - remaining.Value):0} / {entitlement.Value:0} used" : null;
                quotas.Add(new(title, unlimited ? null : 100 - JsonFields.Clamp(percentLeft!.Value), true, reset, detail, unlimited));
            }
            var primaryTitle = free ? (credits ? "AI credits used" : "Chat used") : (credits ? "AI credits used" : "Premium requests used");
            return new(Id, plan, quotas, quotas.Any(q => q.Title == primaryTitle) ? null : "Copilot has not reported its primary plan quota.",
                UpdatedAt: DateTimeOffset.Now) { SignedIn = true };
        }
        catch (CopilotFailure failure) { return UsageSnapshot.Unavailable(Id, failure.Message) with { SignedIn = failure.SignedIn }; }
        catch (Exception error) when (deadline.IsCancellationRequested && !cancellationToken.IsCancellationRequested
                                     && error is OperationCanceledException or IOException or ObjectDisposedException or InvalidOperationException)
        { return UsageSnapshot.Unavailable(Id, "Copilot quota request timed out. Try again later."); }
    }
    public void Dispose() { }
}
