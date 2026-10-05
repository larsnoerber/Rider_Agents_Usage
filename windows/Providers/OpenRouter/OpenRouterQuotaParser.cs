using System.Globalization;
using System.IO;
using System.Text.Json.Nodes;
using AgentMeter.Windows.Core;

namespace AgentMeter.Windows.Providers.OpenRouter;

internal static class OpenRouterQuotaParser
{
    internal static UsageSnapshot Parse(JsonObject response)
    {
        var data = response.Object("data") ?? throw new InvalidDataException("Missing key data.");
        var quotas = new List<UsageQuota>();
        var details = new List<string> { "Scope: the connected OpenRouter API key; shared across clients using this key." };
        var limit = data.Number("limit");
        var remaining = data.Number("limit_remaining");
        var reset = data.Text("limit_reset");
        if (limit is > 0 && remaining.HasValue)
            quotas.Add(new("Key budget remaining", JsonFields.Clamp(remaining.Value / limit.Value * 100), false,
                Detail: $"{Money(remaining.Value)} remaining / {Money(limit.Value)} limit"));
        else if (limit == 0)
            quotas.Add(new("Key budget remaining", 0, false, Detail: "Key spending limit is $0.00."));
        else if (limit.HasValue) quotas.Add(new("Key budget", null, false,
            Detail: "The API did not report the remaining key budget."));
        else quotas.Add(new("Key budget", null, false, DisplayValue: "No limit",
            Detail: "No spending cap is configured for this API key."));
        foreach (var (field, title) in new[] { ("usage_daily", "Spend today"), ("usage_weekly", "Spend this week"),
            ("usage_monthly", "Spend this month") })
            if (data.Number(field) is double spend) quotas.Add(new(title, null, true, DisplayValue: Money(spend)));
        if (quotas.Count == 0) quotas.Add(new("Key budget", null, false,
            Detail: "No finite key budget or spending values were reported."));
        if (reset != null) details.Add($"Budget reset policy: {reset}. The API does not report an exact reset timestamp.");
        foreach (var (field, label) in new[] { ("usage", "Total spend"), ("usage_daily", "Today"),
            ("usage_weekly", "This week"), ("usage_monthly", "This month") })
            if (data.Number(field) is double value) details.Add($"{label}: {Money(value)}");
        if (data.Flag("include_byok_in_limit")) details.Add("The key budget includes usage billed through your own provider keys.");
        var requests = data.Object("free_model_daily_requests");
        if (requests.Number("limit") is > 0 and var requestLimit && requests.Number("remaining") is double requestRemaining)
            details.Add($"Free-model requests reported today: {requestRemaining:0} remaining / {requestLimit:0}. Enforcement depends on account and endpoint.");
        details.Add("Account credit balance is separate from this key budget; no management key is requested.");
        return new("openrouter", data.Flag("is_free_tier") ? "Free tier" : "API credits", quotas,
            limit == null ? "This key has no spending cap. Account credits can still run out."
                : limit > 0 && remaining == null ? "OpenRouter did not report the remaining key budget." : null,
            details, DateTimeOffset.Now) { SignedIn = true };
    }

    private static string Money(double amount) => "$" + amount.ToString("0.00", CultureInfo.InvariantCulture);
}
