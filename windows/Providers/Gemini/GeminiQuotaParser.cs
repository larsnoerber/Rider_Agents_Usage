using System.Globalization;
using System.Text.Json.Nodes;
using AgentMeter.Windows.Core;

namespace AgentMeter.Windows.Providers.Gemini;

internal static class GeminiQuotaParser
{
    public static UsageSnapshot Parse(JsonObject account, JsonObject response)
    {
        var paid = account.Object("paidTier");
        var current = account.Object("currentTier");
        var plan = paid.Text("name") ?? current.Text("name") ?? paid.Text("id") ?? current.Text("id") ?? "Gemini Code Assist";
        var quotas = new List<UsageQuota>();
        foreach (var bucket in (response["buckets"] as JsonArray ?? []).OfType<JsonObject>())
        {
            var fraction = bucket.Number("remainingFraction");
            var amount = bucket.Number("remainingAmount");
            if (fraction == null && amount == null) continue;
            var model = bucket.Text("modelId") ?? "Model quota";
            var type = bucket.Text("tokenType");
            var detail = amount.HasValue ? $"Remaining amount: {amount.Value.ToString("0.##", CultureInfo.InvariantCulture)}" : null;
            if (type != null) detail = string.Join(" · ", new[] { detail, type }.Where(s => s != null));
            // Fractions outside 0..1 are unavailable, never guessed or normalized.
            quotas.Add(new(model + (type == null ? "" : " · " + type) + " remaining", fraction is >= 0 and <= 1 ? fraction * 100 : null,
                false, JsonFields.Date(bucket.Text("resetTime")), detail));
        }
        return new("gemini", plan, quotas, quotas.Count == 0 ? "Google did not report model quota buckets for this CLI account." : null,
            ["Source: Gemini CLI / Gemini Code Assist, using the official local Google session.",
                "These are CLI model quotas. Gemini Apps web and AI Studio API quotas are separate.",
                "Workspace quota access depends on the Cloud project, API access and assigned license.",
                "Open Gemini CLI to renew an expired session. AgentMeter does not refresh or store Google credentials.",
                "Google's internal CLI quota interface can change."], DateTimeOffset.Now) { SignedIn = true };
    }
}
