using System.IO;
using System.Text.Json;
using System.Text.RegularExpressions;
using AgentMeter.Windows.Core;
using AgentMeter.Windows.Core.Processes;

namespace AgentMeter.Windows.Providers.Junie;

internal sealed class JunieReader : IUsageReader
{
    private readonly JunieConnection connection = new();
    public string Id => "junie";
    public Task<UsageSnapshot> ReadAsync(string executablePath, CancellationToken token) => ReadProjectAsync(executablePath, "", token);

    public async Task<UsageSnapshot> ReadProjectAsync(string executablePath, string projectPath, CancellationToken token)
    {
        var path = AgentExecutables.Find(Id, executablePath);
        if (path == null) return UsageSnapshot.Unavailable(Id, "Junie CLI not found. Install Junie or select its executable in Settings.");
        var project = string.IsNullOrWhiteSpace(projectPath) ? Environment.GetFolderPath(Environment.SpecialFolder.UserProfile) : projectPath;
        if (!Directory.Exists(project)) return UsageSnapshot.Unavailable(Id, "Junie project directory does not exist. Update it in Settings.");
        try
        {
            var report = await connection.ReadAsync(path, Path.GetFullPath(project), token);
            var quotas = new List<UsageQuota>();
            var balance = Regex.Match(report.Text, @"(?im)^\s*-\s*Balance left:\s*(?<value>(?:\$\s*)?[0-9]+(?:[.,][0-9]+)?(?:\s*(?:credits?|USD|AI Credits|tokens?))?)\s*$");
            if (balance.Success) quotas.Add(new("Credit balance", null, false, DisplayValue: balance.Groups["value"].Value));
            // The first Tokens/Cost pair belongs to the empty monitoring session; only the all-time section is useful.
            var sections = report.Text.Split("**All-time (this machine)**", StringSplitOptions.None);
            var history = sections.Length > 1 ? sections[1].Split("**License & quota**", StringSplitOptions.None)[0] : "";
            Add(quotas, history, "Tokens", "Tokens · selected project", @"[0-9][0-9,.]*(?:\s*[KMB])?");
            Add(quotas, history, "Cost", "Cost · selected project", @"\$\s*[0-9]+(?:\.[0-9]+)?");
            var license = Regex.Match(report.Text, @"(?m)^- License:\s*(?<value>[^\r\n]{1,120})$");
            return new(Id, license.Success ? license.Groups["value"].Value.Trim() : "Junie CLI", quotas,
                report.Notice ?? (quotas.Count == 0 ? report.Connected == true
                    ? "Connected, but Junie did not report balance or saved usage for this project."
                    : "Junie did not report balance or saved usage for this project." : null),
                ["Statistics come from Junie's built-in /stats command. No model task is submitted.",
                 "Saved usage covers the selected project on this machine. Configure its directory to include your coding sessions.",
                 "Credit balance is shown only when Junie reports it. BYOK connections may have no JetBrains balance."],
                DateTimeOffset.Now) { SignedIn = report.Connected };
        }
        catch (Exception e) when (e is IOException or JsonException or InvalidOperationException or System.ComponentModel.Win32Exception)
        { return UsageSnapshot.Unavailable(Id, "Junie statistics are temporarily unavailable. Check its CLI connection and refresh."); }
    }

    private static void Add(List<UsageQuota> quotas, string text, string label, string title, string pattern)
    {
        var match = Regex.Match(text, @"(?m)^- " + label + @":\s*(?<value>" + pattern + @")\s*$");
        if (match.Success) quotas.Add(new(title, null, true, DisplayValue: match.Groups["value"].Value));
    }

    public void Dispose() => connection.Dispose();
    internal void Pause() => connection.Pause();
}
