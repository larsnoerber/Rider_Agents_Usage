namespace AgentMeter.Windows.Core.History;

internal sealed record UsageObservation(DateTimeOffset At, IReadOnlyDictionary<string, double> Consumed);

// Only numeric quota observations are retained, in memory for this app session.
internal sealed class UsageHistory
{
    private readonly List<UsageObservation> observations = [];
    public IReadOnlyList<UsageObservation> Observations => observations;

    public void Record(UsageSnapshot snapshot)
    {
        if (snapshot.UpdatedAt is not DateTimeOffset at ||
            observations.LastOrDefault()?.At >= at) return;
        var values = snapshot.Quotas.Where(q => !q.Unlimited && q.Percent is double p && double.IsFinite(p))
            .GroupBy(q => q.Title).ToDictionary(g => g.Key, g =>
            {
                var quota = g.First();
                var percent = JsonFields.Clamp(quota.Percent!.Value);
                return quota.Consumed ? percent : 100 - percent;
            });
        if (values.Count == 0) return;
        observations.RemoveAll(point => point.At < at.AddHours(-24));
        observations.Add(new(at, values));
        if (observations.Count > 4096) observations.RemoveRange(0, observations.Count - 4096);
    }
}
