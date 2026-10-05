using System.Globalization;
using System.IO;
using AgentMeter.Windows.Core;
using Microsoft.Data.Sqlite;

namespace AgentMeter.Windows.Providers.Cline;

internal sealed class ClineReader : IUsageReader
{
    private bool disposed;
    public string Id => "cline";

    public async Task<UsageSnapshot> ReadAsync(string executablePath, CancellationToken cancellationToken)
    {
        ObjectDisposedException.ThrowIf(disposed, this);
        var connected = await ClineConnection.ReadAsync(executablePath, cancellationToken);
        var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        var root = Environment.GetEnvironmentVariable("CLINE_DIR") ?? Path.Combine(home, ".cline");
        var data = Environment.GetEnvironmentVariable("CLINE_DATA_DIR") ?? Path.Combine(root, "data");
        var directory = Environment.GetEnvironmentVariable("CLINE_DB_DATA_DIR") ?? Path.Combine(data, "db");
        var file = Path.Combine(directory, "sessions.db");
        if (!File.Exists(file)) return UsageSnapshot.Unavailable(Id, connected == true
            ? "Connected. No local usage recorded yet. Use Cline, then refresh."
            : "No local Cline history yet. Select Sign in to authenticate or configure a provider, then use Cline and refresh.") with { SignedIn = connected };
        try
        {
            using var connection = new SqliteConnection(new SqliteConnectionStringBuilder
            { DataSource = file, Mode = SqliteOpenMode.ReadOnly, Pooling = false, DefaultTimeout = 2 }.ToString());
            await connection.OpenAsync(cancellationToken);
            using var command = connection.CreateCommand();
            // Select numeric usage only; prompts, paths, titles and credentials never leave the store.
            // Sum each session's own usage, not aggregateUsage which includes child sessions.
            command.CommandText = """
                SELECT COUNT(*),
                    SUM(COALESCE(json_extract(metadata_json, '$.usage.totalCost'), json_extract(metadata_json, '$.totalCost'))),
                    SUM(json_extract(metadata_json, '$.usage.inputTokens')),
                    SUM(json_extract(metadata_json, '$.usage.outputTokens')),
                    SUM(json_extract(metadata_json, '$.usage.cacheReadTokens')),
                    SUM(json_extract(metadata_json, '$.usage.cacheWriteTokens'))
                FROM sessions
                WHERE json_valid(metadata_json) AND julianday(started_at) >= julianday('now', '-30 days')
                """;
            using var rows = await command.ExecuteReaderAsync(cancellationToken);
            var quotas = new List<UsageQuota>();
            if (await rows.ReadAsync(cancellationToken))
            {
                var titles = new[] { "Cost · last 30 days", "Input tokens · last 30 days", "Output tokens · last 30 days",
                    "Cache read tokens · last 30 days", "Cache write tokens · last 30 days" };
                for (var i = 1; i <= 5; i++)
                {
                    if (rows.IsDBNull(i)) continue;
                    var amount = rows.GetDouble(i);
                    if (!double.IsFinite(amount) || amount < 0) continue;
                    quotas.Add(new(titles[i - 1], null, true, DisplayValue: amount.ToString(i == 1 ? "$0.00" : "N0", CultureInfo.InvariantCulture)));
                }
            }
            return new(Id, "Local usage", quotas, quotas.Count == 0 ? "No Cline usage recorded in the last 30 days." : null,
                ["Usage covers local Cline sessions started in the last 30 days, including subagents.",
                 "Recorded model costs are estimates. Account credits and subscription limits are separate.",
                 "Connection status comes from Cline's configured provider readiness; local history is separate."], DateTimeOffset.Now)
                { SignedIn = connected };
        }
        catch (Exception error) when (error is SqliteException or IOException or UnauthorizedAccessException)
        { return UsageSnapshot.Unavailable(Id, "Cline history is temporarily unavailable or uses an unsupported format. AgentMeter retries automatically.") with { SignedIn = connected }; }
    }

    public void Dispose() => disposed = true;
}
