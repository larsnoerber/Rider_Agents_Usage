using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;
using System.Windows.Media;
using AgentMeter.Windows.Core;

namespace AgentMeter.Windows.UI.Components;

internal static class WidgetProviderContent
{
    internal static FrameworkElement Create(string id, UsageSnapshot? snapshot, string style, WidgetColorChoice colors, bool showResets = false, bool showFreshness = false, int refreshSeconds = 300)
    {
        var provider = ProviderCatalog.Get(id);
        var label = style == "mini" ? provider.MiniLabel : provider.BarLabel;
        var quotas = snapshot?.Quotas ?? [];
        var primary = id == "copilot" ? quotas.FirstOrDefault(q => q.Title is "Premium requests used" or "AI credits used" or "Chat used")
            : id == "codex" ? quotas.FirstOrDefault(q => q.Title == "5-hour remaining") : quotas.FirstOrDefault();
        var weekly = id == "codex" ? quotas.FirstOrDefault(q => q.Title == "Weekly remaining")
            : id == "gemini" ? quotas.FirstOrDefault(q => q.Title.Contains("weekly", StringComparison.OrdinalIgnoreCase)) : null;
        var text = new TextBlock
        {
            VerticalAlignment = VerticalAlignment.Center,
            TextWrapping = style == "vertical" ? TextWrapping.Wrap : TextWrapping.NoWrap,
            Foreground = colors.TextBrush,
            FontSize = style is "compact" or "mini" ? 12 : 14
        };
        text.Inlines.Add(new Run(label + " | "));
        if (id == "codex") text.Inlines.Add(new Run("D="));
        AddValue(text, primary, colors);
        if (id == "codex")
        {
            text.Inlines.Add(new Run(" - W="));
            AddValue(text, weekly, colors);
        }
        if (style is "classic" or "compact" or "vertical" or "mini")
        {
            var lines = new StackPanel();
            lines.Children.Add(text);
            AddMetadata(lines, snapshot, primary, weekly, colors, showResets && style != "mini", showFreshness, refreshSeconds);
            return lines;
        }
        var content = new StackPanel { VerticalAlignment = VerticalAlignment.Top };
        if (style == "cards")
        {
            content.Children.Add(text);
            AddBar(content, primary, colors);
            if (weekly != null && id == "gemini")
            {
                var weeklyText = new TextBlock
                {
                    FontSize = 10,
                    Foreground = text.Foreground,
                    Margin = new Thickness(0, 5, 0, 0)
                };
                weeklyText.Inlines.Add(new Run("Week "));
                AddValue(weeklyText, weekly, colors);
                content.Children.Add(weeklyText);
            }
            if (weekly != null) AddBar(content, weekly, colors);
        }
        else
        {
            content.Children.Add(new TextBlock
            {
                Text = label,
                Foreground = text.Foreground,
                FontSize = 12,
                HorizontalAlignment = HorizontalAlignment.Center,
                Margin = new Thickness(0, 0, 0, 6)
            });
            var rings = new StackPanel { Orientation = Orientation.Horizontal, HorizontalAlignment = HorizontalAlignment.Center };
            AddRing(rings, primary, weekly == null ? null : "5h", colors);
            if (weekly != null) AddRing(rings, weekly, "Week", colors);
            content.Children.Add(rings);
        }
        AddMetadata(content, snapshot, primary, weekly, colors, showResets, showFreshness, refreshSeconds);
        return new Border
        {
            Child = content,
            CornerRadius = new CornerRadius(8),
            Padding = new Thickness(10, 8, 10, 8),
            Background = colors.BadgeBrush,
            BorderBrush = colors.BorderBrush,
            BorderThickness = new Thickness(1)
        };
    }

    private static void AddMetadata(Panel panel, UsageSnapshot? snapshot, UsageQuota? primary, UsageQuota? weekly,
        WidgetColorChoice colors, bool showResets, bool showFreshness, int refreshSeconds)
    {
        if (showResets)
        {
            foreach (var (quota, label) in new[] { (primary, weekly == null ? "" : "5h: "), (weekly, "Week: ") })
            {
                var reset = WidgetTimeText.Reset(quota);
                if (reset.Length > 0) panel.Children.Add(new TextBlock
                {
                    Text = label + reset,
                    FontSize = 10,
                    Foreground = colors.TextBrush,
                    Margin = new Thickness(0, 4, 0, 0)
                });
            }
        }
        var freshness = showFreshness || snapshot?.IsCached == true ? WidgetTimeText.Freshness(snapshot, refreshSeconds) : "";
        if (freshness.Length > 0) panel.Children.Add(new TextBlock
        {
            Text = freshness,
            FontSize = 10,
            Foreground = new SolidColorBrush(Color.FromRgb(244, 197, 106)),
            Margin = new Thickness(0, 4, 0, 0)
        });
    }

    private static void AddValue(TextBlock text, UsageQuota? quota, WidgetColorChoice colors) =>
        text.Inlines.Add(new Run(quota?.Unlimited == true ? "Unlimited" : quota?.Percent is double p ? $"{p:0}%" : quota?.DisplayValue ?? "—")
        { Foreground = QuotaRing.QuotaBrush(quota, colors) });

    private static void AddBar(Panel panel, UsageQuota? quota, WidgetColorChoice colors)
    {
        if (quota?.Percent is not double percent || quota.Unlimited) return;
        panel.Children.Add(new ProgressBar
        {
            Minimum = 0,
            Maximum = 100,
            Value = JsonFields.Clamp(percent),
            Height = 4,
            Margin = new Thickness(0, 6, 0, 0),
            Foreground = QuotaRing.QuotaBrush(quota, colors),
            Background = colors.TrackBrush,
            BorderThickness = new Thickness(0)
        });
    }

    private static void AddRing(Panel panel, UsageQuota? quota, string? label, WidgetColorChoice colors)
    {
        var column = new StackPanel { Margin = new Thickness(3, 0, 3, 0) };
        var ring = new QuotaRing(quota, colors) { Width = 48, Height = 48 };
        System.Windows.Automation.AutomationProperties.SetName(ring,
            $"{quota?.Title ?? "Quota unavailable"}: {(quota?.Percent is double p ? $"{p:0}%" : quota?.DisplayValue ?? "unavailable")}");
        column.Children.Add(ring);
        column.Children.Add(new TextBlock
        {
            Text = label ?? "",
            FontSize = 10,
            Height = 14,
            Foreground = colors.TextBrush,
            HorizontalAlignment = HorizontalAlignment.Center,
            Margin = new Thickness(0, 3, 0, 0)
        });
        panel.Children.Add(column);
    }
}
