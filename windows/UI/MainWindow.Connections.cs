using System.Diagnostics;
using System.Windows;
using System.Windows.Controls;
using AgentMeter.Windows.Core;
using AgentMeter.Windows.Application;
using AgentMeter.Windows.Settings;
using AgentMeter.Windows.Providers.OpenRouter;

namespace AgentMeter.Windows.UI;

// Official sign-in actions and connection presentation. Credentials never enter settings files.
public partial class MainWindow
{
    private async void OpenRouterConnectClick(object sender, RoutedEventArgs args)
    {
        if (closed) return;
        using var key = OpenRouterKeyInput.SecurePassword;
        if (key.Length == 0) { OpenRouterKeyStatus.Text = "Enter your OpenRouter API key first."; return; }
        ApplyEnteredOpenRouterKey(key);
        SettingsStore.Save(settings);
        await ApplyOpenRouterConnection();
    }

    private void ApplyEnteredOpenRouterKey(System.Security.SecureString key)
    {
        openRouterPersistenceFailed = !OpenRouterKeyStore.Save(key);
        coordinator.SetOpenRouterSessionKey(key);
        OpenRouterKeyInput.Clear();
        settings = settings with { EnabledProviders = settings.EnabledProviders.Append("openrouter").Distinct().ToArray() };
        updatingOpenRouterSelection = true;
        try { choices["openrouter"].IsChecked = true; }
        finally { updatingOpenRouterSelection = false; }
        RebuildCards();
        widget?.Apply(settings);
        OpenRouterConnectionState.Text = "Checking connection...";
        OpenRouterKeyStatus.Text = openRouterPersistenceFailed
            ? "Windows could not save the key. Checking the connection for this session..."
            : "Key saved in Windows Credential Manager. Checking the connection...";
    }

    private async void OpenRouterClearClick(object sender, RoutedEventArgs args)
    {
        if (closed) return;
        if (!OpenRouterKeyStore.Delete())
        {
            OpenRouterKeyStatus.Text = "Windows could not remove the saved key. Try again.";
            return;
        }
        OpenRouterKeyInput.Clear();
        coordinator.SetOpenRouterSessionKey(null);
        openRouterPersistenceFailed = false;
        OpenRouterKeyStatus.Text = "Saved key removed. Existing OpenCode keys can still be detected.";
        await ApplyOpenRouterConnection();
    }

    private async Task ApplyOpenRouterConnection()
    {
        OpenRouterConnectButton.IsEnabled = false;
        OpenRouterClearButton.IsEnabled = false;
        try { await coordinator.ApplyAsync(settings); }
        finally
        {
            if (!closed) { OpenRouterConnectButton.IsEnabled = true; OpenRouterClearButton.IsEnabled = true; }
        }
    }
    private async void Login(string provider)
    {
        if (closed || loginRunning) return;
        if (provider == "cline-docs")
        {
            AgentLogin.OpenInstallation("cline");
            return;
        }
        if (provider == "openrouter")
        {
            OpenDashboard();
            NavigateSettingsPage("openrouter");
            OpenRouterKeyInput.Focus();
            return;
        }
        if (provider == "gemini")
        {
            try
            {
                AgentLogin.OpenGeminiApp();
                RefreshStatus.Text = "Gemini Apps opened. Refresh after checking the account.";
            }
            catch (Exception) { RefreshStatus.Text = "Could not open Gemini Apps. Open the app manually, then refresh."; }
            return;
        }
        if (provider == "gemini-api")
        {
            try { AgentLogin.OpenWebsite(provider); }
            catch (Exception) { RefreshStatus.Text = "Could not open the provider website. Check your default browser."; }
            return;
        }
        if (provider == "gemini-cli") provider = "gemini";
        loginRunning = true;
        foreach (var card in cards.Values) card.SetLoginEnabled(false);
        RefreshButton.IsEnabled = false;
        try
        {
            var executable = await Task.Run(() => AgentLogin.FindExecutable(provider, settings), lifetime.Token);
            if (executable == null)
            {
                MessageBox.Show(this, "The official agent executable was not found. The installation guide will open. Install the agent or enter its executable path in Settings, then select Sign in again.",
                    "Agent required", MessageBoxButton.OK, MessageBoxImage.Information);
                AgentLogin.OpenInstallation(provider);
                return;
            }
            RefreshStatus.Text = "Complete sign-in with the official agent…";
            if (provider == "copilot")
                await coordinator.SignInCopilotAsync(executable, code =>
                {
                    var dialog = new DeviceLoginWindow(code) { Owner = this };
                    return Task.FromResult(dialog.ShowDialog() == true);
                }, lifetime.Token);
            else
            {
                var process = AgentLogin.Start(provider, executable);
                if (process != null)
                {
                    // The official login owns its window. Return control immediately, even if its prompt stays open.
                    _ = RefreshAfterExternalLoginAsync(process);
                    if (provider is "junie" or "opencode" or "cline" or "kilo")
                        _ = coordinator.MonitorConnectionAsync(provider, lifetime.Token);
                    await coordinator.RefreshAsync();
                }
            }
        }
        catch (Exception) when (lifetime.IsCancellationRequested) { }
        catch (Exception)
        {
            if (!closed) MessageBox.Show(this, "Sign-in could not be completed. Check the official agent, executable path and connection, then try again.",
                "Sign-in unavailable", MessageBoxButton.OK, MessageBoxImage.Information);
        }
        finally
        {
            loginRunning = false;
            if (!closed)
            {
                foreach (var card in cards.Values) card.SetLoginEnabled(true);
                RefreshButton.IsEnabled = true;
                RefreshStatus.Text = "Ready · Sign-in continues in the official agent";
            }
        }
    }

    private async Task RefreshAfterExternalLoginAsync(Process process)
    {
        using (process)
        using (var wait = CancellationTokenSource.CreateLinkedTokenSource(lifetime.Token))
        {
            wait.CancelAfter(TimeSpan.FromMinutes(3));
            try
            {
                await process.WaitForExitAsync(wait.Token);
                if (!closed) await coordinator.RefreshAsync();
            }
            catch (OperationCanceledException) { }
            catch (Exception)
            {
                if (!closed) RefreshStatus.Text = "Ready · Refresh after completing sign-in";
            }
        }
    }

    private void UpdateConnectionState(UsageSnapshot snapshot)
    {
        var label = snapshot.ProviderId switch
        {
            "codex" => CodexConnectionState,
            "copilot" => CopilotConnectionState,
            "claude" => ClaudeConnectionState,
            "cursor" => CursorConnectionState,
            "gemini" => GeminiConnectionState,
            "openrouter" => OpenRouterConnectionState,
            "kilo" => KiloConnectionState,
            "cline" => ClineConnectionState,
            "opencode" => OpenCodeConnectionState,
            "junie" => JunieConnectionState,
            _ => null
        };
        if (label == null) return;
        connectionStates.TryGetValue(snapshot.ProviderId, out var connected);
        if (snapshot.SignedIn.HasValue) connected = snapshot.SignedIn;
        connectionStates[snapshot.ProviderId] = connected;
        label.Text = connected == true ? "Connected"
            : connected == false ? "Not connected" : "Connection not verified";
        label.Foreground = new System.Windows.Media.SolidColorBrush((System.Windows.Media.Color)System.Windows.Media.ColorConverter.ConvertFromString(
            connected == true ? "#67DAB1" : connected == false ? "#F4C56A" : "#9BAAC1"));
    }
}
