using AgentMeter.Windows.Core;

namespace AgentMeter.Windows.UI.Components;

internal static class WidgetTimeText
{
    internal static string Reset(UsageQuota? quota)
    {
        if (quota?.ResetsAt is not DateTimeOffset reset || quota.Unlimited) return "";
        var time = reset - DateTimeOffset.Now;
        if (time <= TimeSpan.Zero) return "Reset due";
        if (time.TotalDays >= 1) return $"Reset in {(int)time.TotalDays}d {time.Hours}h";
        if (time.TotalHours >= 1) return $"Reset in {(int)time.TotalHours}h {time.Minutes}m";
        return $"Reset in {Math.Max(1, (int)Math.Ceiling(time.TotalMinutes))}m";
    }

    internal static string Freshness(UsageSnapshot? snapshot, int refreshSeconds)
    {
        if (snapshot == null) return "Waiting";
        if (snapshot.IsCached) return "Saved · " + snapshot.UpdatedAt?.ToLocalTime().ToString("g");
        if (snapshot.Quotas.Count == 0) return "Unavailable";
        if (snapshot.UpdatedAt is not DateTimeOffset updated) return "Age unknown";
        var age = DateTimeOffset.Now - updated;
        if (age.TotalSeconds > Math.Max(120, refreshSeconds * 2 + 60)
            || snapshot.Notice?.Contains("stale", StringComparison.OrdinalIgnoreCase) == true) return "Stale";
        return "";
    }
}
