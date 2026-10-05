namespace AgentMeter.Windows.Core;

/// <summary>Stable persisted IDs shared by settings and provider selection.</summary>
internal static class ProviderIds
{
    // Changing these values requires a settings migration; labels can change independently.
    public static IReadOnlyList<string> All { get; } = Array.AsReadOnly(new[]
    {
        "codex", "copilot", "claude", "cursor", "gemini", "openrouter", "kilo", "cline", "opencode", "junie"
    });

    public static bool IsKnown(string id) => All.Contains(id);
}
