using System.IO;
using System.Net;
using System.Text;
using System.Text.Json.Nodes;
using System.Text.RegularExpressions;
using AgentMeter.Windows.Core;

namespace AgentMeter.Windows.Providers.Cursor;

internal sealed class CursorReader : IUsageReader
{
    private readonly ProviderHttp http = new(["cursor.com", "api2.cursor.sh"]);
    public string Id => "cursor";

    public async Task<UsageSnapshot> ReadAsync(string executablePath, CancellationToken cancellationToken)
    {
        var path = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "Cursor", "auth.json");
        if (!File.Exists(path)) return UsageSnapshot.Unavailable(Id, "Sign in with Cursor Agent to create its official local auth store.") with { SignedIn = false };
        try
        {
            string? accessToken = JsonNode.Parse(await File.ReadAllTextAsync(path, cancellationToken)).Text("accessToken");
            var parts = accessToken?.Split('.');
            if (parts?.Length != 3) return UsageSnapshot.Unavailable(Id, "Cursor session unavailable. Sign in again with Cursor Agent.") with { SignedIn = false };
            var encoded = parts[1].Replace('-', '+').Replace('_', '/');
            encoded = encoded.PadRight((encoded.Length + 3) / 4 * 4, '=');
            var payload = JsonNode.Parse(Encoding.UTF8.GetString(Convert.FromBase64String(encoded)));
            var user = payload.Text("sub")?.Split('|').Last();
            if (user == null || !Regex.IsMatch(user, "^[A-Za-z0-9._-]+$"))
                return UsageSnapshot.Unavailable(Id, "Cursor session format unavailable.") with { SignedIn = false };
            if (payload.Number("exp") is not double expires || expires <= DateTimeOffset.UtcNow.ToUnixTimeSeconds() + 60)
                return UsageSnapshot.Unavailable(Id, "Cursor session expired. Sign in again with Cursor Agent.") with { SignedIn = false };
            JsonObject response;
            try
            {
                try
                {
                    response = await http.SendAsync("https://cursor.com/api/usage-summary", new Dictionary<string, string>
                    {
                        ["Cookie"] = $"WorkosCursorSessionToken={user}%3A%3A{accessToken}",
                        ["Origin"] = "https://cursor.com", ["Referer"] = "https://cursor.com/dashboard"
                    }, cancellationToken);
                }
                catch (ProviderHttpFailure failure) when (failure.Status == HttpStatusCode.Forbidden)
                {
                    response = await http.SendAsync("https://api2.cursor.sh/aiserver.v1.DashboardService/GetCurrentPeriodUsage",
                        new Dictionary<string, string> { ["Authorization"] = "Bearer " + accessToken,
                            ["Connect-Protocol-Version"] = "1" }, cancellationToken, post: true);
                }
            }
            finally { accessToken = null; }
            var individual = response.Object("individualUsage");
            var plan = individual.Object("plan") ?? response.Object("planUsage");
            var pooled = response.Object("teamUsage").Object("pooled");
            var quota = new[] { plan, individual.Object("overall"), pooled }.FirstOrDefault(q =>
                q.Number("totalPercentUsed") != null || (q.Number("limit") > 0 && (q.Number("used") != null || q.Number("remaining") != null)));
            var limit = quota.Number("limit");
            var used = quota.Number("used") ?? (limit.HasValue && quota.Number("remaining") is double left ? Math.Max(0, limit.Value - left) : null);
            var percent = quota.Number("totalPercentUsed") ?? (limit > 0 && used.HasValue ? used * 100 / limit : null);
            var billingEnd = response.Text("billingCycleEnd");
            var reset = double.TryParse(billingEnd, out var milliseconds) ? JsonFields.Unix(milliseconds / 1000) : JsonFields.Date(billingEnd);
            var quotas = new List<UsageQuota>();
            if (percent.HasValue) quotas.Add(new(ReferenceEquals(quota, pooled) ? "Shared team pool used" : "Included plan used",
                JsonFields.Clamp(percent.Value), true, reset, used.HasValue && limit.HasValue ? $"${used / 100:0.00} / ${limit / 100:0.00}" : null));
            foreach (var (key, title) in new[] { ("autoPercentUsed", "Auto used"), ("apiPercentUsed", "API used") })
                if (plan.Number(key) is double value) quotas.Add(new(title, JsonFields.Clamp(value), true));
            var details = individual.Object("onDemand").Number("used") is double onDemand
                ? new[] { $"On-demand: ${onDemand / 100:0.00}" } : [];
            return new(Id, response.Text("membershipType"), quotas, percent == null ? "Cursor did not report a finite plan quota." : null,
                details, DateTimeOffset.Now) { SignedIn = true };
        }
        catch (ProviderHttpFailure failure) { return UsageSnapshot.Unavailable(Id, failure.SafeMessage("Cursor")) with
            { SignedIn = failure.Status is HttpStatusCode.Unauthorized or HttpStatusCode.Forbidden ? false : null }; }
    }
    public void Dispose() => http.Dispose();
}
