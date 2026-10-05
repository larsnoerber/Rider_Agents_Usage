using System.IO;
using System.Security.Cryptography;
using System.Text.Json;

namespace AgentMeter.Windows.Providers.OpenRouter;

internal static class OpenRouterCredentials
{
    internal static async Task<string?> ReadAsync(CancellationToken token)
    {
        // OpenCode's official provider credential store. Read only the exact OpenRouter entry.
        var dataHome = Environment.GetEnvironmentVariable("XDG_DATA_HOME");
        var directory = !string.IsNullOrWhiteSpace(dataHome) && Path.IsPathFullyQualified(dataHome)
            ? dataHome : Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), ".local", "share");
        var path = Path.Combine(directory, "opencode", "auth.json");
        if (!File.Exists(path)) return null;
        await using var stream = new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.ReadWrite,
            4096, FileOptions.Asynchronous);
        if (stream.Length > 1_048_576) throw new InvalidDataException("Credential store is too large.");
        var bytes = new byte[checked((int)stream.Length)];
        try
        {
            await stream.ReadExactlyAsync(bytes, token);
            using var document = JsonDocument.Parse(bytes);
            if (!document.RootElement.TryGetProperty("openrouter", out var entry)
                || entry.ValueKind != JsonValueKind.Object
                || !entry.TryGetProperty("type", out var type) || type.GetString() != "api"
                || !entry.TryGetProperty("key", out var value) || value.ValueKind != JsonValueKind.String) return null;
            var key = value.GetString();
            return key is { Length: > 0 and <= 4096 } && !key.Any(char.IsWhiteSpace) ? key : null;
        }
        finally { CryptographicOperations.ZeroMemory(bytes); }
    }
}
