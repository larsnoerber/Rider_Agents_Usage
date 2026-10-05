using System.IO;
using System.Net;
using System.Net.Http;
using System.Text.Json;
using System.Security;
using System.Runtime.InteropServices;
using AgentMeter.Windows.Core;

namespace AgentMeter.Windows.Providers.OpenRouter;

internal sealed class OpenRouterReader : IUsageReader
{
    private readonly ProviderHttp http = new(["openrouter.ai"]);
    private bool disposed;
    private readonly object credentialLock = new();
    private SecureString? sessionKey;
    public string Id => "openrouter";

    internal void SetSessionKey(SecureString? value)
    {
        lock (credentialLock)
        {
            if (disposed) return;
            sessionKey?.Dispose();
            sessionKey = value?.Copy();
        }
    }

    private string? ReadSessionKey()
    {
        lock (credentialLock)
        {
            if (sessionKey == null || disposed) return null;
            var buffer = Marshal.SecureStringToBSTR(sessionKey);
            try { return Marshal.PtrToStringBSTR(buffer); }
            finally { Marshal.ZeroFreeBSTR(buffer); }
        }
    }

    public async Task<UsageSnapshot> ReadAsync(string executablePath, CancellationToken cancellationToken)
    {
        if (disposed) throw new ObjectDisposedException(nameof(OpenRouterReader));
        string? key = null;
        var headers = new Dictionary<string, string>();
        try
        {
            key = ReadSessionKey() ?? OpenRouterKeyStore.Read() ?? await OpenRouterCredentials.ReadAsync(cancellationToken);
            if (key == null) return UsageSnapshot.Unavailable(Id,
                "Enter your OpenRouter API key in Settings and select Connect. An existing OpenCode /connect key is also detected automatically.")
                with { SignedIn = false };
            if (key.Any(char.IsWhiteSpace)) return UsageSnapshot.Unavailable(Id,
                "The API key contains whitespace. Enter the complete key without spaces or line breaks.") with { SignedIn = false };
            headers["Authorization"] = "Bearer " + key;
            var response = await http.SendAsync("https://openrouter.ai/api/v1/key", headers, cancellationToken);
            // Do not display the returned key label, account IDs or other identity fields.
            return OpenRouterQuotaParser.Parse(response);
        }
        catch (ProviderHttpFailure failure)
        {
            return UsageSnapshot.Unavailable(Id, failure.Status is HttpStatusCode.Unauthorized or HttpStatusCode.Forbidden
                ? "OpenRouter rejected the key. Replace it in Settings or reconnect in OpenCode, then refresh."
                : failure.SafeMessage("OpenRouter")) with
            { SignedIn = failure.Status is HttpStatusCode.Unauthorized or HttpStatusCode.Forbidden ? false : null };
        }
        catch (Exception error) when (error is IOException or UnauthorizedAccessException or JsonException or HttpRequestException
            or InvalidOperationException)
        {
            return UsageSnapshot.Unavailable(Id, "OpenRouter usage is temporarily unavailable. Check the key and connection; AgentMeter retries automatically.");
        }
        finally { headers.Clear(); key = null; }
    }

    public void Dispose()
    {
        lock (credentialLock) { disposed = true; sessionKey?.Dispose(); sessionKey = null; }
        http.Dispose();
    }
}
