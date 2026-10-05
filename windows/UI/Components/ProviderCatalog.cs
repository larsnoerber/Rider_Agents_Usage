namespace AgentMeter.Windows.UI.Components;

internal sealed record ProviderPresentation(string Id, string Name, string BarLabel, string MiniLabel, string Accent);

/// <summary>One presentation catalog keeps dashboard, bar and configuration labels consistent.</summary>
internal static class ProviderCatalog
{
    public static IReadOnlyList<ProviderPresentation> All { get; } = Array.AsReadOnly(new[]
    {
        new ProviderPresentation("codex", "OpenAI Codex", "OpenAi", "OAI", "#67DAB1"),
        new ProviderPresentation("copilot", "GitHub Copilot", "Copilot", "CP", "#84B8FF"),
        new ProviderPresentation("claude", "Claude Code", "Claude", "CL", "#F3B38D"),
        new ProviderPresentation("cursor", "Cursor", "Cursor", "CU", "#D1E2F5"),
        new ProviderPresentation("gemini", "Gemini", "Gemini", "GE", "#A4B8FF"),
        new ProviderPresentation("openrouter", "OpenRouter", "OpenRouter", "OR", "#D1B1FF"),
        new ProviderPresentation("kilo", "Kilo", "Kilo", "KI", "#F4CD6B"),
        new ProviderPresentation("cline", "Cline", "Cline", "CN", "#79D5CD"),
        new ProviderPresentation("opencode", "OpenCode", "OpenCode", "OC", "#CDD3DF"),
        new ProviderPresentation("junie", "Junie", "Junie", "JU", "#D3F277")
    });

    public static ProviderPresentation Get(string id) => All.FirstOrDefault(provider => provider.Id == id)
        ?? new(id, id, id, id, "#9BAAC1");
}
