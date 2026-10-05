using System.Windows.Media;

namespace AgentMeter.Windows.UI.Components;

internal sealed record WidgetColorChoice(string Id, string Label, string Surface, string Badge,
    string Border, string Track, string Text, string Accent)
{
    internal Brush SurfaceBrush => Brush(Surface);
    internal Brush BadgeBrush => Brush(Badge);
    internal Brush BorderBrush => Brush(Border);
    internal Brush TrackBrush => Brush(Track);
    internal Brush TextBrush => Brush(Text);
    internal Brush AccentBrush => Brush(Accent);
    private static Brush Brush(string color) => new SolidColorBrush((Color)ColorConverter.ConvertFromString(color));
}

internal static class WidgetColors
{
    internal static readonly WidgetColorChoice[] Choices =
    [
        new("midnight", "Midnight", "#070D16", "#18212E", "#303A49", "#303A49", "#BFD8F6", "#67DAB1"),
        new("ocean", "Ocean", "#081A28", "#102E43", "#28516B", "#23465D", "#D0EDFF", "#68CFFF"),
        new("forest", "Forest", "#0B1B15", "#173329", "#345B49", "#2A4C3C", "#D6F0DE", "#9DDD73"),
        new("plum", "Plum", "#1C1026", "#32203F", "#604176", "#4D335F", "#EFDAFF", "#C6A0FF"),
        new("graphite", "Graphite", "#161719", "#282A2E", "#4A4D54", "#40434B", "#E7E9EF", "#AEC2E8")
    ];

    internal static WidgetColorChoice Resolve(string? id) => Choices.FirstOrDefault(choice => choice.Id == id) ?? Choices[0];
}
