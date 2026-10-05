using System.Text.RegularExpressions;

namespace AgentMeter.Windows.Core.Processes;

internal static class AgentStatistics
{
    public static string PlainText(string text) => Regex.Replace(text, @"\x1B\[[0-?]*[ -/]*[@-~]", "");

    public static bool? Connected(string text)
    {
        var counts = Regex.Matches(PlainText(text), @"\b(?<count>\d+)\s+(?:credentials?|environment variables?)\b");
        return counts.Count == 0 ? null : counts.Any(m => int.TryParse(m.Groups["count"].Value, out var count) && count > 0);
    }

    public static void AddReported(List<UsageQuota> quotas, string text, string label, string title, string pattern)
    {
        var match = Regex.Match(PlainText(text), @"(?m)^\s*[│|]?\s*" + Regex.Escape(label)
            + @"\s+[│|]?\s*(?<value>" + pattern + @")\s*[│|]?\s*$");
        if (match.Success) quotas.Add(new(title, null, true, DisplayValue: match.Groups["value"].Value));
    }
}
