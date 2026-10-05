using System.Globalization;
using System.Text.Json;
using System.Text.RegularExpressions;
using AgentMeter.Windows.Core;
using AgentMeter.Windows.Core.Processes;

namespace AgentMeter.Windows.Providers.OpenCode;

// The desktop v2 CLI uses JSON while legacy installations return formatted tables.
internal static class OpenCodeStatistics
{
    public static bool UsesJson(string version)
    {
        var match = Regex.Match(version, @"(?:^|\s)v?(?<major>\d+)\.[0-9]+\.");
        return match.Success && int.TryParse(match.Groups["major"].Value, out var major) && major >= 2;
    }

    public static bool? Connected(string output, bool jsonFormat)
    {
        if (!jsonFormat) return AgentStatistics.Connected(output);
        using var json = JsonDocument.Parse(output);
        if (json.RootElement.ValueKind != JsonValueKind.Array) return null;
        return json.RootElement.EnumerateArray().Any(provider => provider.TryGetProperty("connections", out var connections)
            && connections.ValueKind == JsonValueKind.Array && connections.GetArrayLength() > 0);
    }

    public static void AddUsage(List<UsageQuota> quotas, string output, bool jsonFormat)
    {
        if (!jsonFormat)
        {
            AgentStatistics.AddReported(quotas, output, "Total Cost", "Cost · last 30 days", @"\$[0-9]+(?:\.[0-9]+)?");
            AgentStatistics.AddReported(quotas, output, "Input", "Input tokens · last 30 days", @"[0-9]+(?:\.[0-9]+)?[KMB]?");
            AgentStatistics.AddReported(quotas, output, "Output", "Output tokens · last 30 days", @"[0-9]+(?:\.[0-9]+)?[KMB]?");
            AgentStatistics.AddReported(quotas, output, "Cache Read", "Cached tokens · last 30 days", @"[0-9]+(?:\.[0-9]+)?[KMB]?");
            return;
        }

        using var json = JsonDocument.Parse(output);
        var root = json.RootElement;
        AddNumber(quotas, root, "cost", "Cost · last 30 days", cost: true);
        if (!root.TryGetProperty("tokens", out var tokens)) return;
        AddNumber(quotas, tokens, "input", "Input tokens · last 30 days");
        AddNumber(quotas, tokens, "output", "Output tokens · last 30 days");
        if (tokens.TryGetProperty("cache", out var cache)) AddNumber(quotas, cache, "read", "Cached tokens · last 30 days");
    }

    private static void AddNumber(List<UsageQuota> quotas, JsonElement json, string key, string title, bool cost = false)
    {
        // Only numeric usage leaves the parser; connection labels and account identifiers are discarded.
        if (json.TryGetProperty(key, out var value) && value.TryGetDouble(out var number) && double.IsFinite(number) && number >= 0)
            quotas.Add(new(title, null, true, DisplayValue: number.ToString(cost ? "$0.00" : "0", CultureInfo.InvariantCulture)));
    }
}
