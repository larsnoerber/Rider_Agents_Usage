using System.IO;
using System.Text.Json.Nodes;
using AgentMeter.Windows.Core;

namespace AgentMeter.Windows.Providers.Gemini;

internal static class GeminiLocalConfiguration
{
    public static string DirectoryPath()
    {
        var configured = Environment.GetEnvironmentVariable("GEMINI_CLI_HOME");
        return Path.Combine(!string.IsNullOrWhiteSpace(configured) && Path.IsPathFullyQualified(configured)
            ? configured : Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), ".gemini");
    }

    public static async Task<(string? AuthType, string? Project)> ReadAsync(string root, string configuredProject, CancellationToken token)
    {
        string? authType = null;
        var settings = Path.Combine(root, "settings.json");
        if (File.Exists(settings))
        {
            if (new FileInfo(settings).Length > 131_072) throw new InvalidDataException();
            var data = JsonNode.Parse(await File.ReadAllTextAsync(settings, token));
            authType = data.Object("security").Object("auth").Text("selectedType") ?? data.Text("selectedAuthType");
        }
        var primary = Environment.GetEnvironmentVariable("GOOGLE_CLOUD_PROJECT");
        var project = !string.IsNullOrWhiteSpace(configuredProject) ? configuredProject.Trim()
            : !string.IsNullOrWhiteSpace(primary) ? primary : Environment.GetEnvironmentVariable("GOOGLE_CLOUD_PROJECT_ID");
        var env = Path.Combine(root, ".env");
        if (string.IsNullOrWhiteSpace(project) && File.Exists(env))
        {
            if (new FileInfo(env).Length > 131_072) throw new InvalidDataException();
            project = ReadProject(await File.ReadAllLinesAsync(env, token));
        }
        return (authType, string.IsNullOrWhiteSpace(project) ? null : project.Trim());
    }

    internal static string? ReadProject(IEnumerable<string> lines)
    {
        string? primary = null, alternate = null;
        foreach (var line in lines)
        {
            var text = line.Trim();
            if (text.StartsWith("export ", StringComparison.Ordinal)) text = text[7..].TrimStart();
            var separator = text.IndexOf('=');
            if (separator < 0) continue;
            var key = text[..separator].Trim();
            if (key is not "GOOGLE_CLOUD_PROJECT" and not "GOOGLE_CLOUD_PROJECT_ID") continue;
            var value = text[(separator + 1)..].Trim();
            if (value.Length >= 2 && (value[0] is '\'' or '"') && value[^1] == value[0]) value = value[1..^1];
            else value = value.Split('#', 2)[0].Trim();
            if (string.IsNullOrWhiteSpace(value)) continue;
            if (key == "GOOGLE_CLOUD_PROJECT") primary = value; else alternate = value;
        }
        return primary ?? alternate;
    }
}
