using AgentMeter.Windows.Core;
using AgentMeter.Windows.Core.Processes;
using AgentMeter.Windows.Providers.Gemini;
using AgentMeter.Windows.Providers.Junie;
using AgentMeter.Windows.Settings;

namespace AgentMeter.Windows.Application;

internal sealed partial class UsageRefreshCoordinator
{
    // Both full refresh and login monitoring use this path, so credential and cache handling cannot drift apart.
    private Task<UsageSnapshot> ReadProviderAsync(IUsageReader reader, UsageSettings selected, CancellationToken token) =>
        Task.Run(async () =>
        {
            UsageSnapshot snapshot;
            try
            {
                var path = selected.GetExecutablePath(reader.Id);
                if (reader.Id == "codex")
                {
                    var resolved = AgentExecutables.Find(reader.Id, path);
                    if (resolved == null)
                        return savedUsage.Apply(UsageSnapshot.Unavailable(reader.Id,
                            "Official Codex CLI not found. Install it or select its executable in Settings, then sign in.")
                            with
                        { SignedIn = false });
                    path = resolved;
                }
                snapshot = reader switch
                {
                    GeminiReader gemini => await gemini.ReadWorkspaceAsync(selected.GeminiProjectId, token),
                    JunieReader junie => await junie.ReadProjectAsync(path, selected.JunieProjectPath, token),
                    _ => await reader.ReadAsync(path, token)
                };
            }
            catch (Exception) when (token.IsCancellationRequested) { throw new OperationCanceledException(token); }
            catch (Exception)
            {
                // Provider failures must still complete the widget batch; never expose raw exception text.
                snapshot = UsageSnapshot.Unavailable(reader.Id, "Usage unavailable. Check the agent sign-in, executable and connection.");
            }
            token.ThrowIfCancellationRequested();
            return savedUsage.Apply(snapshot);
        }, token);

    private async Task PublishSnapshotAsync(UsageSnapshot snapshot, int version, CancellationToken token)
    {
        if (disposed || token.IsCancellationRequested || version != generation) return;
        await dispatcher.InvokeAsync(() =>
        {
            // Check again on the UI thread: settings may change while this callback is queued.
            if (!disposed && version == generation && !token.IsCancellationRequested) Updated?.Invoke(snapshot);
        });
    }
}
