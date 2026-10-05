using System.Diagnostics;
using System.IO;
using System.Text;
using System.Text.Json.Nodes;
using AgentMeter.Windows.Core;
using AgentMeter.Windows.Core.Processes;
using AgentsUsage.VisualStudio.Core.Processes;

namespace AgentMeter.Windows.Providers.Copilot;

internal sealed class CopilotConnection : IDisposable
{
    private readonly Process process;
    private readonly ProcessJob job = new();
    private int nextId;

    public CopilotConnection(string path)
    {
        process = new Process { StartInfo = AgentExecutables.StartInfo(path, "--stdio") };
        try
        {
            process.ErrorDataReceived += (_, _) => { };
            process.Start();
            job.Add(process);
            process.BeginErrorReadLine();
        }
        catch { Dispose(); throw; }
    }

    public async Task InitializeAsync(CancellationToken token)
    {
        await RequestAsync("initialize", new JsonObject
        {
            ["processId"] = Environment.ProcessId,
            ["rootUri"] = null,
            ["capabilities"] = new JsonObject { ["window"] = new JsonObject { ["showDocument"] = new JsonObject { ["support"] = true } } },
            ["initializationOptions"] = new JsonObject
            {
                ["editorInfo"] = new JsonObject { ["name"] = "AgentMeter", ["version"] = "1.0" },
                ["editorPluginInfo"] = new JsonObject { ["name"] = "AgentMeter", ["version"] = typeof(CopilotConnection).Assembly.GetName().Version?.ToString() }
            }
        }, token);
        await NotifyAsync("initialized", new JsonObject(), token);
        await NotifyAsync("workspace/didChangeConfiguration", new JsonObject
        {
            ["settings"] = new JsonObject { ["telemetry"] = new JsonObject { ["telemetryLevel"] = "off" } }
        }, token);
    }

    public async Task<JsonObject> RequestAsync(string method, JsonObject parameters, CancellationToken token, bool allowLoginBrowser = false)
    {
        int id = ++nextId;
        await SendAsync(new JsonObject { ["jsonrpc"] = "2.0", ["id"] = id, ["method"] = method, ["params"] = parameters }, token);
        while (true)
        {
            var message = await ReceiveAsync(token);
            if (message.Text("method") is string serverMethod && message["id"] != null)
            {
                var reply = new JsonObject { ["jsonrpc"] = "2.0", ["id"] = message["id"]!.DeepClone() };
                switch (serverMethod)
                {
                    case "workspace/configuration":
                        var values = new JsonArray();
                        if (message.Object("params")?["items"] is JsonArray items)
                            foreach (var _ in items) values.Add((JsonNode?)null);
                        reply["result"] = values;
                        break;
                    case "client/registerCapability":
                    case "window/workDoneProgress/create": reply["result"] = null; break;
                    case "window/showDocument":
                        var url = message.Object("params").Text("uri");
                        var success = allowLoginBrowser && Uri.TryCreate(url, UriKind.Absolute, out var uri)
                            && uri.Scheme == "https" && uri.Host == "github.com" && uri.AbsolutePath == "/login/device";
                        if (success) Process.Start(new ProcessStartInfo(url!) { UseShellExecute = true })?.Dispose();
                        reply["result"] = new JsonObject { ["success"] = success };
                        break;
                    default: reply["error"] = new JsonObject { ["code"] = -32601, ["message"] = "Method unavailable" }; break;
                }
                await SendAsync(reply, token);
            }
            else if (message.Number("id") == id)
            {
                if (message.Object("error") is JsonObject error)
                {
                    var reason = error.Text("message") ?? "";
                    throw new CopilotFailure(reason.Contains("not signed in", StringComparison.OrdinalIgnoreCase)
                        ? "Sign in with the Copilot agent to load your quota."
                        : error["code"]?.ToString() == "-32601" ? "Update the official Copilot Language Server to support this request."
                        : "Copilot could not complete this request. Check your sign-in and connection.")
                        { SignedIn = reason.Contains("not signed in", StringComparison.OrdinalIgnoreCase) ? false : null };
                }
                return message.Object("result") ?? new JsonObject();
            }
        }
    }

    private Task NotifyAsync(string method, JsonObject parameters, CancellationToken token) =>
        SendAsync(new JsonObject { ["jsonrpc"] = "2.0", ["method"] = method, ["params"] = parameters }, token);

    private async Task SendAsync(JsonObject message, CancellationToken token)
    {
        var body = Encoding.UTF8.GetBytes(message.ToJsonString());
        var header = Encoding.ASCII.GetBytes($"Content-Length: {body.Length}\r\n\r\n");
        await process.StandardInput.BaseStream.WriteAsync(header, token);
        await process.StandardInput.BaseStream.WriteAsync(body, token);
        await process.StandardInput.BaseStream.FlushAsync(token);
    }

    private async Task<JsonObject> ReceiveAsync(CancellationToken token)
    {
        var stream = process.StandardOutput.BaseStream;
        var header = new List<byte>();
        var single = new byte[1];
        while (true)
        {
            if (await stream.ReadAsync(single, token) == 0) throw new IOException("Agent connection closed.");
            header.Add(single[0]);
            if (header.Count > 8192) throw new InvalidDataException("Invalid agent header.");
            if (header.Count >= 4 && header[^4] == 13 && header[^3] == 10 && header[^2] == 13 && header[^1] == 10) break;
        }
        var lengthHeader = Encoding.ASCII.GetString(header.ToArray()).Split("\r\n")
            .FirstOrDefault(line => line.StartsWith("Content-Length:", StringComparison.OrdinalIgnoreCase));
        if (!int.TryParse(lengthHeader?.Split(':')[1].Trim(), out var length) || length is < 1 or > 2_097_152)
            throw new InvalidDataException("Invalid agent frame.");
        var body = new byte[length];
        await stream.ReadExactlyAsync(body, token);
        return JsonNode.Parse(body) as JsonObject ?? throw new InvalidDataException("Invalid agent response.");
    }

    public void Dispose()
    {
        job.Dispose();
        try { if (!process.HasExited) process.Kill(entireProcessTree: true); }
        catch (Exception error) when (error is InvalidOperationException or System.ComponentModel.Win32Exception) { }
        process.Dispose();
    }
}

internal sealed class CopilotFailure(string message) : Exception(message)
{
    public bool? SignedIn { get; init; }
}
