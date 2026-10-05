using System.Diagnostics;
using System.IO;

namespace AgentMeter.Windows.Core.Processes;

internal static class AgentExecutables
{
    public static string? Find(string provider, string configuredPath = "")
    {
        if (!string.IsNullOrWhiteSpace(configuredPath)) return Validate(configuredPath);
        string[] names = provider switch
        {
            "codex" => ["codex.exe", "codex.cmd"],
            "claude" => ["claude.exe", "claude.cmd"],
            "cursor" => ["cursor-agent.exe", "cursor-agent.cmd", "agent.exe", "agent.cmd"],
            "copilot" => ["copilot-language-server.exe", "copilot-language-server.cmd"],
            "gemini" => ["gemini.exe", "gemini.cmd"],
            "kilo" => ["kilo.exe", "kilo.cmd"],
            "cline" => ["cline.exe", "cline.cmd"],
            "opencode" => ["opencode.exe", "opencode.cmd", "opencode-cli.exe"],
            "junie" => ["junie.exe", "junie.cmd", "junie.bat"],
            "node" => ["node.exe"],
            _ => []
        };
        var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        var directories = (Environment.GetEnvironmentVariable("PATH") ?? "").Split(Path.PathSeparator)
            .Concat([Path.Combine(home, ".local", "bin"), Path.Combine(home, ".cursor", "bin"), Path.Combine(home, ".opencode", "bin"),
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "npm")]);
        if (provider == "node") directories = directories.Concat([
            Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles), "nodejs"),
            Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "Programs", "nodejs")]);
        foreach (var directory in directories)
        {
            if (string.IsNullOrWhiteSpace(directory)) continue;
            foreach (var name in names)
                if (Validate(Path.Combine(directory.Trim().Trim('"'), name)) is string found) return found;
        }
        if (provider == "opencode")
        {
            // The desktop app bundles a separate CLI; its GUI executable is not a polling command.
            var roaming = Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData);
            var desktopCli = Path.Combine(roaming, "ai.opencode.desktop", "cli");
            try
            {
                if (Directory.Exists(desktopCli))
                    foreach (var version in Directory.EnumerateDirectories(desktopCli).OrderByDescending(File.GetLastWriteTimeUtc))
                        if (Validate(Path.Combine(version, "opencode-cli.exe")) is string cli) return cli;
            }
            catch (Exception error) when (error is IOException or UnauthorizedAccessException) { }
            var bundled = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                "Programs", "@opencodedesktop", "resources", "opencode-cli.exe");
            if (Validate(bundled) is string desktop) return desktop;
        }
        // Existing official ACP packages are usable without starting or installing an IDE.
        var jetBrains = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "JetBrains");
        if (!Directory.Exists(jetBrains)) return null;
        var package = provider switch { "codex" => "codex-acp", "claude" => "claude-acp", "copilot" => "github-copilot", "cursor" => "cursor", "kilo" => "kilo", "cline" => "cline", "opencode" => "opencode", "junie" => "junie", _ => "" };
        var relative = provider switch
        {
            "codex" => "node_modules/.bin/codex.cmd",
            "claude" => "node_modules/@anthropic-ai/claude-agent-sdk-win32-x64/claude.exe",
            "copilot" => "node_modules/@github/copilot-language-server-win32-x64/copilot-language-server.exe",
            "cursor" => "dist-package/cursor-agent.cmd",
            "kilo" => "kilo.exe",
            "cline" => "node_modules/.bin/cline.cmd",
            "opencode" => "opencode.exe",
            "junie" => "junie/junie.exe",
            _ => ""
        };
        try
        {
            foreach (var ide in Directory.EnumerateDirectories(jetBrains).OrderDescending())
            {
                if (provider == "node")
                {
                    var runtimes = Path.Combine(ide, "acp-agents", ".runtimes", "node");
                    if (Directory.Exists(runtimes))
                        foreach (var runtime in Directory.EnumerateDirectories(runtimes).OrderByDescending(File.GetLastWriteTimeUtc))
                            if (Validate(Path.Combine(runtime, "node.exe")) is string node) return node;
                    continue;
                }
                var root = Path.Combine(ide, "acp-agents", package);
                if (!Directory.Exists(root)) continue;
                foreach (var version in Directory.EnumerateDirectories(root).OrderByDescending(File.GetLastWriteTimeUtc))
                    if (Validate(Path.Combine(version, relative)) is string found) return found;
            }
        }
        catch (Exception error) when (error is IOException or UnauthorizedAccessException) { }
        return null;
    }

    private static string? Validate(string path)
    {
        // Command wrappers go through cmd.exe. Reject its metacharacters before constructing a command.
        if (path.IndexOfAny(['"', '\r', '\n', '%', '!', '^', '&', '|', '<', '>']) >= 0) return null;
        var extension = Path.GetExtension(path);
        if (!new[] { ".exe", ".cmd", ".bat" }.Contains(extension, StringComparer.OrdinalIgnoreCase)) return null;
        try { return File.Exists(path) ? Path.GetFullPath(path) : null; }
        catch (Exception error) when (error is IOException or ArgumentException or UnauthorizedAccessException) { return null; }
    }

    public static ProcessStartInfo StartInfo(string path, string arguments, bool interactive = false)
    {
        if (Validate(path) == null) throw new IOException("Agent executable unavailable.");
        var wrapper = !path.EndsWith(".exe", StringComparison.OrdinalIgnoreCase);
        return new ProcessStartInfo
        {
            FileName = wrapper ? Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.System), "cmd.exe") : path,
            Arguments = wrapper ? $"/d /s /c \"\"{path}\" {arguments}\"" : arguments,
            UseShellExecute = interactive, CreateNoWindow = !interactive,
            RedirectStandardInput = !interactive, RedirectStandardOutput = !interactive, RedirectStandardError = !interactive,
            WorkingDirectory = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile)
        };
    }
}
