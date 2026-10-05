using AgentMeter.Windows.Providers.Copilot;

namespace AgentMeter.Windows.Application;

internal sealed partial class UsageRefreshCoordinator
{
    // Copilot's official device login owns authentication; it runs while polling is paused.
    public async Task SignInCopilotAsync(string path, Func<string, Task<bool>> showCode, CancellationToken token)
    {
        if (disposed || signingIn) return;
        signingIn = true;
        cancellation.Cancel();
        cancellation.Dispose();
        cancellation = new();
        using var login = CancellationTokenSource.CreateLinkedTokenSource(token, cancellation.Token);
        login.CancelAfter(TimeSpan.FromMinutes(5));
        var entered = false;
        try
        {
            await gate.WaitAsync(login.Token);
            entered = true;
            await Task.Run(() => CopilotSignIn.RunAsync(path,
                code => dispatcher.InvokeAsync(() => showCode(code)).Task.Unwrap(), login.Token), login.Token);
        }
        finally
        {
            if (entered) gate.Release();
            signingIn = false;
            if (!disposed) await RefreshAsync();
        }
    }

    // Interactive agents can stay open after login; watch readiness independently of process exit.
    public async Task MonitorConnectionAsync(string provider, CancellationToken token)
    {
        using var monitor = CancellationTokenSource.CreateLinkedTokenSource(token);
        monitor.CancelAfter(TimeSpan.FromMinutes(3));
        try
        {
            while (!disposed && settings.EnabledProviders.Contains(provider))
            {
                await Task.Delay(TimeSpan.FromSeconds(3), monitor.Token);
                if (await RefreshConnectionAsync(provider)) break;
            }
        }
        catch (OperationCanceledException) { }
    }
}
