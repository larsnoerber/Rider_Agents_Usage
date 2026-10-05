using AgentMeter.Windows.Core;

namespace AgentMeter.Windows.Providers.Copilot;

/// <summary>The official language server owns device authentication and its process lifetime.</summary>
internal static class CopilotSignIn
{
    public static async Task RunAsync(string path, Func<string, Task<bool>> showCode, CancellationToken token)
    {
        using var connection = new CopilotConnection(path);
        using var registration = token.Register(connection.Dispose);
        await connection.InitializeAsync(token);
        var result = await connection.RequestAsync("signIn", new(), token);
        var code = result.Text("userCode");
        if (code == null || !await showCode(code)) return;
        var command = result.Object("command");
        if (command?.Text("command") != "github.copilot.finishDeviceFlow")
            throw new CopilotFailure("This Copilot version uses an unsupported sign-in flow. Update the official Language Server.");
        await connection.RequestAsync("workspace/executeCommand", command, token, allowLoginBrowser: true);

        // Some versions acknowledge the command before authorization finishes; keep the server alive until ready.
        while (true)
        {
            var status = await connection.RequestAsync("checkStatus", new(), token);
            if (status.Text("status") is "OK" or "AlreadySignedIn") return;
            await Task.Delay(TimeSpan.FromSeconds(2), token);
        }
    }
}
