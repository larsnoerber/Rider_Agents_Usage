using System.Diagnostics;
using System.IO;
using AgentMeter.Windows.Core.Processes;
using AgentMeter.Windows.Settings;

namespace AgentMeter.Windows.Application;

internal static class AgentLogin
{
    internal static string WebProviderUrl(string provider) => provider switch
    { "gemini" => "https://gemini.google.com/",
        "gemini-api" => "https://aistudio.google.com/rate-limit?timeRange=last-28-days",
        "openrouter" => "https://opencode.ai/docs/providers/#openrouter",
        _ => throw new ArgumentException("Unsupported web provider.", nameof(provider)) };

    public static void OpenWebsite(string provider) =>
        Process.Start(new ProcessStartInfo(WebProviderUrl(provider)) { UseShellExecute = true })?.Dispose();

    public static void OpenGeminiApp()
    {
        var executable = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "Google", "Gemini", "Gemini.exe");
        if (File.Exists(executable))
            Process.Start(new ProcessStartInfo(executable) { UseShellExecute = true })?.Dispose();
        else
        {
            // Edge is also a supported local session source for the Gemini Apps reader.
            var edge = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFilesX86),
                "Microsoft", "Edge", "Application", "msedge.exe");
            if (File.Exists(edge))
                Process.Start(new ProcessStartInfo(edge, "https://gemini.google.com/usage") { UseShellExecute = true })?.Dispose();
            else OpenWebsite("gemini");
        }
    }

    public static string? FindExecutable(string provider, UsageSettings settings) =>
        AgentExecutables.Find(provider, settings.GetExecutablePath(provider));

    public static Process? Start(string provider, string executable) => Process.Start(AgentExecutables.StartInfo(executable,
        provider switch { "claude" => "auth login", "gemini" => "", "kilo" => "auth login", "cline" => "auth", "opencode" => "auth login", "junie" => "--skip-update-check", _ => "login" }, interactive: true));

    public static void OpenInstallation(string provider)
    {
        var url = provider switch
        {
            "codex" => "https://developers.openai.com/codex/cli/",
            "claude" => "https://code.claude.com/docs/en/setup",
            "cursor" => "https://cursor.com/docs/cli/installation",
            "copilot" => "https://github.com/github/copilot-language-server-release",
            "gemini" => "https://geminicli.com/docs/get-started/installation/",
            "kilo" => "https://kilo.ai/docs/code-with-ai/platforms/cli",
            "cline" => "https://docs.cline.bot/cline-cli/overview",
            "opencode" => "https://opencode.ai/docs/cli/",
            "junie" => "https://junie.jetbrains.com/docs/junie-cli.html",
            _ => throw new ArgumentException("Unsupported provider.", nameof(provider))
        };
        Process.Start(new ProcessStartInfo(url) { UseShellExecute = true })?.Dispose();
    }
}
