namespace AgentMeter.Windows.UI.Components;

internal sealed record WidgetStyleChoice(string Id, string Label, string Description);

internal static class WidgetStyles
{
    internal static IReadOnlyList<WidgetStyleChoice> Choices { get; } =
    [
        new("classic", "Classic", "Slim horizontal strip with quota text and dividers."),
        new("mini", "Mini", "Provider initials and quota percentages in the smallest strip."),
        new("compact", "Compact", "Small rounded strip with tighter spacing."),
        new("cards", "Cards", "Separate provider tiles with compact quota bars."),
        new("circles", "Circles", "Circular quota meters with colored percentages."),
        new("vertical", "Vertical", "Providers stacked in a narrow column.")
    ];
}
