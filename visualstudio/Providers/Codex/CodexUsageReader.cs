using System;
using System.Diagnostics;
using System.Globalization;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using AgentsUsage.VisualStudio.Core.Processes;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace AgentsUsage.VisualStudio.Providers.Codex
{
    /// <summary>Reads account metadata and quota only through the signed-in Codex CLI app-server.</summary>
    internal sealed class CodexUsageReader : IDisposable
    {
        private readonly object gate = new object();
        private Process activeProcess;
        private ProcessJob activeJob;
        private bool disposed;

        public async Task<CodexUsage> ReadAsync(string configuredPath, CancellationToken cancellationToken)
        {
            try
            {
                using (var deadline = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken))
                {
                    deadline.CancelAfter(TimeSpan.FromSeconds(45));
                    var command = ResolveCommand(configuredPath);
                    var start = new ProcessStartInfo(command, "app-server --stdio")
                    {
                        UseShellExecute = false, CreateNoWindow = true,
                        RedirectStandardInput = true, RedirectStandardOutput = true, RedirectStandardError = true
                    };
                    if (command.EndsWith(".cmd", StringComparison.OrdinalIgnoreCase) || command.EndsWith(".bat", StringComparison.OrdinalIgnoreCase))
                    {
                        start.FileName = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.System), "cmd.exe");
                        start.Arguments = "/d /s /c \"\"" + command + "\" app-server --stdio\"";
                    }
                    using (var job = new ProcessJob())
                    using (var child = new Process { StartInfo = start })
                    {
                        // Discard stderr without buffering or logging account/provider data.
                        child.ErrorDataReceived += (sender, args) => { };
                        lock (gate)
                        {
                            if (disposed) throw new OperationCanceledException(cancellationToken);
                            child.Start();
                            try { job.Add(child); }
                            catch { Stop(child); throw; }
                            child.BeginErrorReadLine();
                            activeProcess = child;
                            activeJob = job;
                        }
                        using (deadline.Token.Register(job.Dispose))
                        {
                            try
                            {
                                await RequestAsync(child, 1, "initialize", new JObject
                                {
                                    ["clientInfo"] = new JObject { ["name"] = "agents-usage-visualstudio", ["version"] = typeof(CodexUsageReader).Assembly.GetName().Version.ToString() }
                                }, deadline.Token).ConfigureAwait(false);
                                await child.StandardInput.WriteLineAsync("{\"method\":\"initialized\"}").ConfigureAwait(false);
                                string accountPlan = null;
                                try
                                {
                                    var account = await RequestAsync(child, 2, "account/read", new JObject { ["refreshToken"] = false }, deadline.Token).ConfigureAwait(false);
                                    accountPlan = Text((account["account"] as JObject)?["planType"]);
                                }
                                catch (InvalidDataException) { /* Account metadata is optional; quota can still be available. */ }
                                var result = await RequestAsync(child, 3, "account/rateLimits/read", null, deadline.Token).ConfigureAwait(false);
                                return Parse(result, accountPlan);
                            }
                            catch (IOException) when (deadline.IsCancellationRequested)
                            {
                                throw new OperationCanceledException(deadline.Token);
                            }
                            finally
                            {
                                Stop(child);
                                lock (gate)
                                {
                                    if (activeProcess == child) { activeProcess = null; activeJob = null; }
                                }
                            }
                        }
                    }
                }
            }
            catch (OperationCanceledException) when (cancellationToken.IsCancellationRequested) { throw; }
            catch (Exception error) when (error is OperationCanceledException || error is IOException || error is InvalidOperationException
                                           || error is System.ComponentModel.Win32Exception || error is JsonException || error is ArgumentException)
            {
                return new CodexUsage { Error = error is OperationCanceledException
                    ? "Codex CLI did not respond within 45 seconds."
                    : "Codex quota is unavailable. Check the CLI path and sign in with codex login." };
            }
        }

        private static async Task<JObject> RequestAsync(Process child, int id, string method, JObject parameters, CancellationToken cancellationToken)
        {
            cancellationToken.ThrowIfCancellationRequested();
            await child.StandardInput.WriteLineAsync(new JObject { ["id"] = id, ["method"] = method, ["params"] = parameters }.ToString(Formatting.None)).ConfigureAwait(false);
            while (true)
            {
                cancellationToken.ThrowIfCancellationRequested();
                var line = await child.StandardOutput.ReadLineAsync().ConfigureAwait(false);
                cancellationToken.ThrowIfCancellationRequested();
                if (line == null) throw new IOException("Codex closed the quota connection.");
                JObject message;
                try { message = JObject.Parse(line); }
                catch (JsonException) { continue; }
                if ((string)message["id"] != id.ToString(CultureInfo.InvariantCulture)) continue;
                if (message["error"] != null) throw new InvalidDataException("Codex quota request failed.");
                return message["result"] as JObject ?? new JObject();
            }
        }

        private static CodexUsage Parse(JObject result, string accountPlan)
        {
            var limits = (result["rateLimitsByLimitId"] as JObject)?["codex"] as JObject ?? result["rateLimits"] as JObject;
            var usage = new CodexUsage { Plan = Text(limits?["planType"]) ?? accountPlan };
            if (limits != null)
            {
                usage.FiveHourLeft = Remaining(limits["primary"]);
                usage.WeeklyLeft = Remaining(limits["secondary"]);
                usage.FiveHourReset = Reset(limits["primary"]);
                usage.WeeklyReset = Reset(limits["secondary"]);
                usage.Credits = Number((limits["credits"] as JObject)?["balance"]);
            }
            if (!usage.FiveHourLeft.HasValue && !usage.WeeklyLeft.HasValue)
                usage.Error = "Codex has not reported a 5-hour or weekly quota for this account.";
            return usage;
        }

        private static int? Remaining(JToken window)
        {
            var used = Number((window as JObject)?["usedPercent"]);
            return used.HasValue ? (int?)(100 - Math.Round(Math.Max(0, Math.Min(100, used.Value)))) : null;
        }

        private static DateTimeOffset? Reset(JToken window)
        {
            var seconds = Number((window as JObject)?["resetsAt"]);
            if (!seconds.HasValue || seconds < -62135596800d || seconds > 253402300799d) return null;
            return DateTimeOffset.FromUnixTimeSeconds((long)seconds.Value);
        }

        private static double? Number(JToken token)
        {
            if (token == null || (token.Type != JTokenType.Integer && token.Type != JTokenType.Float && token.Type != JTokenType.String)) return null;
            return double.TryParse((string)token, NumberStyles.Float, CultureInfo.InvariantCulture, out var number)
                   && !double.IsNaN(number) && !double.IsInfinity(number) ? (double?)number : null;
        }

        private static string Text(JToken token) => token?.Type == JTokenType.String && !string.IsNullOrWhiteSpace((string)token) ? (string)token : null;

        private static string ResolveCommand(string configuredPath)
        {
            var command = configuredPath?.Trim();
            if (!string.IsNullOrEmpty(command))
            {
                if (command.IndexOfAny(new[] { '"', '\r', '\n' }) >= 0 || !File.Exists(command))
                    throw new ArgumentException("The configured CLI path is unavailable.");
                return Path.GetFullPath(command);
            }
            foreach (var directory in (Environment.GetEnvironmentVariable("PATH") ?? "").Split(Path.PathSeparator))
            {
                if (string.IsNullOrWhiteSpace(directory)) continue;
                foreach (var file in new[] { "codex.exe", "codex.cmd", "codex.bat" })
                {
                    var candidate = Path.Combine(directory.Trim().Trim('"'), file);
                    if (File.Exists(candidate)) return candidate;
                }
            }
            throw new IOException("Codex CLI was not found.");
        }

        private static void Stop(Process process)
        {
            try { if (!process.HasExited) process.Kill(); }
            catch (InvalidOperationException) { }
            catch (System.ComponentModel.Win32Exception) { }
        }

        public void Dispose()
        {
            lock (gate) { disposed = true; activeJob?.Dispose(); if (activeProcess != null) Stop(activeProcess); }
        }
    }
}
