using System.Diagnostics;
using System.IO;
using System.Text;
using System.Text.Json.Nodes;
using AgentMeter.Windows.Core;
using AgentMeter.Windows.Core.Processes;
using AgentsUsage.VisualStudio.Core.Processes;

namespace AgentMeter.Windows.Providers.Claude;

internal static class ClaudeAuthStatus
{
    public static async Task<UsageSnapshot> ReadAsync(string configuredPath, CancellationToken cancellationToken)
    {
        const string unavailable = "Claude subscription credentials are unavailable. Sign in with the official Claude Code agent.";
        var executable = AgentExecutables.Find("claude", configuredPath);
        if (executable == null) return UsageSnapshot.Unavailable("claude", unavailable) with { SignedIn = false };
        using var timeout = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        timeout.CancelAfter(TimeSpan.FromSeconds(10));
        using var job = new ProcessJob();
        using var process = new Process { StartInfo = AgentExecutables.StartInfo(executable, "auth status --json") };
        try
        {
            timeout.Token.ThrowIfCancellationRequested();
            process.Start();
            job.Add(process);
            using var stop = timeout.Token.Register(() => Stop(process));
            var error = ReadBoundedAsync(process.StandardError, timeout.Token);
            var output = await ReadBoundedAsync(process.StandardOutput, timeout.Token);
            await process.WaitForExitAsync(timeout.Token);
            await error;
            var status = JsonNode.Parse(output);
            var loggedIn = status?["loggedIn"] is JsonValue value && value.TryGetValue<bool>(out var signedIn) && signedIn;
            return FromStatus(loggedIn, status.Text("authMethod"), status.Text("subscriptionType"));
        }
        catch (Exception) when (!cancellationToken.IsCancellationRequested)
        { return UsageSnapshot.Unavailable("claude", unavailable); }
        finally { Stop(process); }
    }

    internal static UsageSnapshot FromStatus(bool loggedIn, string? method, string? plan) =>
        new("claude", loggedIn && method == "api_key" ? "Anthropic API" : plan, [],
            !loggedIn ? "Sign in with the official Claude Code agent."
                : method == "api_key" ? "Signed in with an API key. Pro/Max subscription quotas do not apply to this login."
                : "Claude is signed in, but subscription quota credentials are not readable in the official local store.",
            UpdatedAt: DateTimeOffset.Now) { SignedIn = loggedIn };

    private static async Task<string> ReadBoundedAsync(StreamReader reader, CancellationToken token)
    {
        var result = new StringBuilder();
        var buffer = new char[2048];
        int count;
        while ((count = await reader.ReadAsync(buffer.AsMemory(), token)) > 0)
        {
            if (result.Length + count > 65536) throw new IOException("Agent status exceeds the size limit.");
            result.Append(buffer, 0, count);
        }
        return result.ToString();
    }

    private static void Stop(Process process)
    { try { if (!process.HasExited) process.Kill(entireProcessTree: true); } catch (Exception) { } }
}
