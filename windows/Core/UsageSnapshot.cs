namespace AgentMeter.Windows.Core;

// Percent follows the provider's direction; money/tokens use DisplayValue rather than an invented limit.
internal sealed record UsageQuota(string Title, double? Percent, bool Consumed,
    DateTimeOffset? ResetsAt = null, string? Detail = null, bool Unlimited = false, string? DisplayValue = null);

internal sealed record UsageSnapshot(string ProviderId, string? Plan, IReadOnlyList<UsageQuota> Quotas,
    string? Notice = null, IReadOnlyList<string>? Details = null, DateTimeOffset? UpdatedAt = null)
{
    // null means an interrupted/failed read did not establish authentication either way.
    public bool? SignedIn { get; init; }
    // A saved measurement must not be used as evidence of a current authenticated session.
    public bool IsCached { get; init; }
    public static UsageSnapshot Unavailable(string id, string notice) => new(id, null, [], notice);
}

internal interface IUsageReader : IDisposable
{
    string Id { get; }
    Task<UsageSnapshot> ReadAsync(string executablePath, CancellationToken cancellationToken);
}
