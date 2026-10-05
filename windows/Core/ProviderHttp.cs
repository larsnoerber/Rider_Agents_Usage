using System.Net;
using System.IO;
using System.Net.Http;
using System.Text;
using System.Text.Json.Nodes;

namespace AgentMeter.Windows.Core;

internal sealed class ProviderHttp(string[] allowedHosts) : IDisposable
{
    private readonly HttpClient client = new(new HttpClientHandler { AllowAutoRedirect = false, UseCookies = false })
    { Timeout = TimeSpan.FromSeconds(25) };

    public async Task<JsonObject> SendAsync(string url, IReadOnlyDictionary<string, string> headers,
        CancellationToken token, bool post = false, JsonObject? body = null)
    {
        using var deadline = CancellationTokenSource.CreateLinkedTokenSource(token);
        deadline.CancelAfter(TimeSpan.FromSeconds(25));
        token = deadline.Token;
        var uri = new Uri(url);
        if (uri.Scheme != "https" || !allowedHosts.Contains(uri.Host, StringComparer.OrdinalIgnoreCase))
            throw new InvalidOperationException("Provider destination is unavailable.");
        using var request = new HttpRequestMessage(post ? HttpMethod.Post : HttpMethod.Get, uri);
        foreach (var header in headers) request.Headers.TryAddWithoutValidation(header.Key, header.Value);
        request.Headers.Accept.ParseAdd("application/json");
        if (post) request.Content = new StringContent(body?.ToJsonString() ?? "{}", Encoding.UTF8, "application/json");
        using var response = await client.SendAsync(request, HttpCompletionOption.ResponseHeadersRead, token);
        if (!response.IsSuccessStatusCode) throw new ProviderHttpFailure(response.StatusCode);
        // Bound responses and never include response bodies or headers in errors/logs.
        await using var stream = await response.Content.ReadAsStreamAsync(token);
        using var buffer = new MemoryStream();
        var chunk = new byte[8192];
        int count;
        while ((count = await stream.ReadAsync(chunk, token)) > 0)
        {
            if (buffer.Length + count > 2_097_152) throw new InvalidDataException("Provider response too large.");
            buffer.Write(chunk, 0, count);
        }
        buffer.Position = 0;
        return await JsonNode.ParseAsync(buffer, cancellationToken: token) as JsonObject
            ?? throw new InvalidDataException("Provider response unavailable.");
    }

    public void Dispose() => client.Dispose();
}

internal sealed class ProviderHttpFailure(HttpStatusCode status) : Exception
{
    public string SafeMessage(string provider) => Status switch
    {
        HttpStatusCode.Unauthorized or HttpStatusCode.Forbidden => $"{provider} rejected the session. Sign in again in {provider}.",
        HttpStatusCode.TooManyRequests => $"{provider} usage is rate limited. Try again later.",
        _ => $"{provider} quota request failed (HTTP {(int)Status})."
    };
    public HttpStatusCode Status { get; } = status;
}
