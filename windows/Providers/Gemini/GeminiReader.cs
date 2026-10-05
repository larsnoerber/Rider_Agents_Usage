using System.IO;
using System.Net;
using System.Text.Json;
using System.Text.Json.Nodes;
using AgentMeter.Windows.Core;

namespace AgentMeter.Windows.Providers.Gemini;

internal sealed class GeminiReader : IUsageReader
{
    private readonly ProviderHttp http = new(["cloudcode-pa.googleapis.com"]);
    private readonly GeminiWebQuotaReader? web;
    public GeminiReader(bool enableWebFallback = true)
    {
        if (enableWebFallback) web = new GeminiWebQuotaReader();
    }
    public string Id => "gemini";
    public Task<UsageSnapshot> ReadAsync(string executablePath, CancellationToken cancellationToken) =>
        ReadWorkspaceAsync("", cancellationToken);

    public async Task<UsageSnapshot> ReadWorkspaceAsync(string configuredProject, CancellationToken token)
    {
        var cli = await ReadCliWorkspaceAsync(configuredProject, token);
        if (cli.Quotas.Count > 0) return cli;
        if (web == null) return cli;

        var fallback = await web.ReadAsync(token);
        if (fallback.Quotas.Count > 0) return fallback with
        {
            Details = (fallback.Details ?? []).Append("Gemini CLI / Workspace did not report quota, so AgentMeter used the Gemini Apps web fallback.").ToArray()
        };
        var details = (cli.Details ?? []).Concat(fallback.Details ?? []).ToArray();
        // The main action opens Apps; unrelated CLI setup advice belongs in details.
        if (!string.IsNullOrWhiteSpace(cli.Notice)) details = details.Append("Gemini CLI: " + cli.Notice).ToArray();
        var signedIn = cli.SignedIn == true || fallback.SignedIn == true ? true
            : cli.SignedIn == false && fallback.SignedIn == false ? false : (bool?)null;
        return fallback with { Details = details, SignedIn = signedIn };
    }

    private async Task<UsageSnapshot> ReadCliWorkspaceAsync(string configuredProject, CancellationToken token)
    {
        token.ThrowIfCancellationRequested();
        var root = GeminiLocalConfiguration.DirectoryPath();
        var file = Path.Combine(root, "oauth_creds.json");
        if (!File.Exists(file))
            return UsageSnapshot.Unavailable(Id, "Gemini CLI Google sign-in not found. Open Gemini CLI and choose Sign in with Google. Encrypted-only credential stores are not supported.")
                with { SignedIn = false };
        try
        {
            var configuration = await GeminiLocalConfiguration.ReadAsync(root, configuredProject, token);
            if (configuration.AuthType is not null and not "oauth-personal")
                return UsageSnapshot.Unavailable(Id, "Select Sign in with Google in Gemini CLI. API-key and Vertex AI quotas use a different integration.");
            if (configuration.Project == null)
                return UsageSnapshot.Unavailable(Id, "Set the Gemini Workspace Cloud project ID in Settings or GOOGLE_CLOUD_PROJECT, then refresh.");
            if (configuration.Project.All(char.IsDigit))
                return UsageSnapshot.Unavailable(Id, "Use the string Google Cloud project ID, not the numeric project number.");
            string? accessToken;
            // Read only the official CLI store. Never refresh, copy or persist its credentials.
            await using (var stream = File.OpenRead(file))
            {
                if (stream.Length > 131_072) throw new InvalidDataException();
                using var auth = await JsonDocument.ParseAsync(stream, cancellationToken: token);
                var data = auth.RootElement;
                accessToken = data.TryGetProperty("access_token", out var value) && value.ValueKind == JsonValueKind.String
                    ? value.GetString() : null;
                if (data.TryGetProperty("expiry_date", out var expiry) && expiry.ValueKind == JsonValueKind.Number &&
                    expiry.TryGetInt64(out var expires) && expires <= DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() + 30_000)
                    return UsageSnapshot.Unavailable(Id, "Gemini CLI session expired. Open Gemini CLI to renew the Google session, then refresh.")
                        with { SignedIn = false };
            }
            if (string.IsNullOrWhiteSpace(accessToken))
                return UsageSnapshot.Unavailable(Id, "Gemini CLI has no usable Google access token. Sign in with Google in the official CLI.")
                    with { SignedIn = false };
            var headers = new Dictionary<string, string> { ["Authorization"] = "Bearer " + accessToken };
            try
            {
                var project = configuration.Project;
                var account = await http.SendAsync("https://cloudcode-pa.googleapis.com/v1internal:loadCodeAssist", headers,
                    token, post: true, body: new JsonObject
                    {
                        ["cloudaicompanionProject"] = project, ["mode"] = "HEALTH_CHECK",
                        ["metadata"] = new JsonObject { ["ideType"] = "GEMINI_CLI", ["platform"] = "WINDOWS_AMD64",
                            ["pluginType"] = "GEMINI", ["duetProject"] = project }
                    });
                project = account.Text("cloudaicompanionProject") ?? project;
                var quota = await http.SendAsync("https://cloudcode-pa.googleapis.com/v1internal:retrieveUserQuota", headers,
                    token, post: true, body: new JsonObject { ["project"] = project });
                return GeminiQuotaParser.Parse(account, quota);
            }
            finally { headers.Clear(); accessToken = null; }
        }
        catch (ProviderHttpFailure failure)
        {
            return UsageSnapshot.Unavailable(Id, failure.Status == HttpStatusCode.Forbidden
                ? "Google denied quota access. Check the Workspace project, Gemini for Cloud API and assigned Code Assist license with your administrator."
                : failure.SafeMessage("Gemini CLI")) with
            { SignedIn = failure.Status == HttpStatusCode.Unauthorized ? false : null };
        }
        catch (Exception error) when (error is IOException or UnauthorizedAccessException or JsonException)
        { return UsageSnapshot.Unavailable(Id, "Gemini CLI local configuration is unavailable. Check the official CLI sign-in and refresh."); }
    }
    public void Dispose() { http.Dispose(); web?.Dispose(); }
}
