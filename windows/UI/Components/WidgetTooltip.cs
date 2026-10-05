using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using AgentMeter.Windows.Core;

namespace AgentMeter.Windows.UI.Components;

internal static class WidgetTooltip
{
    public static Border CreatePopup(string name, UsageSnapshot? snapshot, bool locked = false, int refreshSeconds = 300)
    {
        var panel = new StackPanel { MinWidth = 280, MaxWidth = 360 };
        var header = new DockPanel { Margin = new Thickness(0, 0, 0, 10) };
        var plan = Text(snapshot?.Plan ?? "Plan unavailable", "#9BAAC1", 12);
        plan.MaxWidth = 160;
        plan.Margin = new Thickness(14, 0, 0, 0);
        plan.VerticalAlignment = VerticalAlignment.Center;
        DockPanel.SetDock(plan, Dock.Right);
        header.Children.Add(plan);
        var title = Text(name, "#E9EEF7", 16);
        title.FontWeight = FontWeights.SemiBold;
        header.Children.Add(title);
        panel.Children.Add(header);
        foreach (var quota in snapshot?.Quotas ?? [])
        {
            var remaining = quota.Percent is double percent ? quota.Consumed ? 100 - percent : percent : 100;
            var color = remaining <= 10 ? "#FF737D" : remaining <= 25 ? "#F4C56A" : "#67DAB1";
            var row = new DockPanel { Margin = new Thickness(0, 3, 0, 5) };
            var value = Text(quota.Unlimited ? "Unlimited" : quota.Percent is double p ? $"{p:0}%" : quota.DisplayValue ?? "Unavailable", color, 14);
            value.FontWeight = FontWeights.SemiBold;
            value.Margin = new Thickness(10, 0, 0, 0);
            DockPanel.SetDock(value, Dock.Right);
            row.Children.Add(value);
            row.Children.Add(Text(quota.Title, "#E9EEF7", 12));
            panel.Children.Add(row);
            if (quota.Percent is double finite && !quota.Unlimited)
            {
                panel.Children.Add(new ProgressBar { Minimum = 0, Maximum = 100, Value = JsonFields.Clamp(finite),
                    Height = 6, Foreground = Brush(color), Background = Brush("#2A3A55"), BorderThickness = new Thickness(0),
                    Margin = new Thickness(0, 0, 0, 5) });
                if (quota.Consumed) panel.Children.Add(Text($"{finite:0}% consumed, {100 - JsonFields.Clamp(finite):0}% available", "#9BAAC1", 11));
            }
            if (quota.ResetsAt is DateTimeOffset reset)
                panel.Children.Add(Text($"Resets {reset.LocalDateTime:g}", "#9BAAC1", 11));
            if (quota.Detail is string detail) panel.Children.Add(Text(detail, "#9BAAC1", 11));
            panel.Children.Add(new Border { Height = 8 });
        }
        var freshness = WidgetTimeText.Freshness(snapshot, refreshSeconds);
        if (freshness.Length > 0) panel.Children.Add(Text(freshness == "Stale"
            ? "Expected quota updates are overdue or the last read failed. AgentMeter retries automatically."
            : freshness, "#F4C56A", 12));
        if (snapshot?.Details != null)
            foreach (var detail in snapshot.Details) panel.Children.Add(Text(detail, "#9BAAC1", 11));
        if (!string.IsNullOrWhiteSpace(snapshot?.Notice)) panel.Children.Add(Text(snapshot.Notice, "#9BAAC1", 12));
        panel.Children.Add(new Border { Height = 1, Background = Brush("#435166"), Margin = new Thickness(0, 5, 0, 8) });
        panel.Children.Add(Text(snapshot?.UpdatedAt is DateTimeOffset date ? $"Updated {date.LocalDateTime:g} ({Math.Max(0, (int)(DateTimeOffset.Now - date).TotalMinutes)}m ago)" : "No current quota data", "#9BAAC1", 11));
        panel.Children.Add(Text((locked ? "Position locked\n" : "") + "Click agent to toggle details · Drag to move\nDouble-click for dashboard · Right-click for menu", "#9BAAC1", 10));
        return new Border { Child = panel, Background = Brush("#19212D"), BorderBrush = Brush("#435166"),
            BorderThickness = new Thickness(1), CornerRadius = new CornerRadius(8), Padding = new Thickness(12, 9, 12, 9), MaxWidth = 420 };
    }

    private static TextBlock Text(string text, string color, double size) => new()
    { Text = text, Foreground = Brush(color), FontSize = size, TextWrapping = TextWrapping.Wrap };
    private static SolidColorBrush Brush(string color) => new((Color)ColorConverter.ConvertFromString(color));
}
