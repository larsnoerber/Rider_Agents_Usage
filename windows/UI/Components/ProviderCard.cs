using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using AgentMeter.Windows.Core;
using AgentMeter.Windows.Core.History;

namespace AgentMeter.Windows.UI.Components;

internal sealed class ProviderCard : Border
{
    private readonly StackPanel content = new();
    private readonly Button planBadge = new();
    private readonly StackPanel details = new() { Visibility = Visibility.Collapsed, Margin = new Thickness(0, 14, 0, 0) };
    private readonly StackPanel facts = new();
    private readonly WrapPanel legend = new();
    private readonly UsageHistory history = new();
    private readonly UsageHistoryChart chart;
    private readonly TextBlock timestamp = new();
    private readonly TextBlock summary = new();
    private readonly Button signIn;
    private readonly Brush muted = Brush("#9BAAC1");
    private readonly string providerId;
    private UsageSnapshot? lastSnapshot;
    private bool signedIn;
    public int RefreshSeconds { get; set; } = 300;

    public ProviderCard(string id, string name, string accent, Action<string> login)
    {
        providerId = id;
        chart = new UsageHistoryChart(history);
        Background = Brush("#2D2F32");
        BorderBrush = Brush("#454545");
        BorderThickness = new Thickness(1);
        CornerRadius = new CornerRadius(7);
        Padding = new Thickness(12);
        Margin = new Thickness(0, 0, 0, 8);
        var root = new StackPanel();
        var header = new DockPanel { Margin = new Thickness(0, 0, 0, 6) };
        signIn = new Button { Content = id == "openrouter" ? "Connect" : "Sign in", FontSize = 12,
            Padding = new Thickness(10, 5, 10, 5), Margin = new Thickness(8, 0, 0, 0), VerticalAlignment = VerticalAlignment.Top };
        if (id == "openrouter") signIn.ToolTip = "Enter your OpenRouter API key in Settings.";
        if (id == "cline") signIn.ToolTip = "Sign in or configure your provider through the official Cline CLI.";
        signIn.Click += (_, _) => login(id);
        DockPanel.SetDock(signIn, Dock.Right);
        header.Children.Add(signIn);
        planBadge.Content = "Plan unavailable ▾";
        planBadge.Foreground = Brush("#FFFFFF");
        planBadge.Background = Brush(PlanSurface(providerId));
        planBadge.BorderBrush = Brush(accent);
        planBadge.BorderThickness = new Thickness(1.5);
        planBadge.Padding = new Thickness(10, 5, 10, 5);
        planBadge.FontSize = 12;
        planBadge.FontWeight = FontWeights.SemiBold;
        planBadge.MaxWidth = 190;
        planBadge.Margin = new Thickness(10, 0, 0, 0);
        planBadge.ToolTip = "Show subscription details and usage history";
        planBadge.Click += (_, _) =>
        {
            details.Visibility = details.Visibility == Visibility.Visible ? Visibility.Collapsed : Visibility.Visible;
            if (lastSnapshot != null) Update(lastSnapshot);
        };
        DockPanel.SetDock(planBadge, Dock.Right);
        header.Children.Add(planBadge);
        header.Children.Add(new TextBlock { Text = name, FontSize = 17, FontWeight = FontWeights.SemiBold,
            Foreground = Brush(accent), TextWrapping = TextWrapping.Wrap });
        root.Children.Add(header);
        root.Children.Add(content);
        var snapshotFooter = new Grid { Margin = new Thickness(0, 4, 0, 0) };
        snapshotFooter.ColumnDefinitions.Add(new ColumnDefinition());
        snapshotFooter.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
        summary.Foreground = muted;
        summary.FontSize = 11;
        timestamp.Foreground = muted;
        timestamp.FontSize = 11;
        Grid.SetColumn(timestamp, 1);
        snapshotFooter.Children.Add(summary);
        snapshotFooter.Children.Add(timestamp);
        root.Children.Add(snapshotFooter);
        details.Children.Add(new Border { Height = 1, Background = Brush("#2A3A55"), Margin = new Thickness(0, 0, 0, 12) });
        details.Children.Add(facts);
        if (id == "gemini")
        {
            var cliLogin = new Button { Content = "Sign in to Gemini CLI",
                HorizontalAlignment = HorizontalAlignment.Left, Margin = new Thickness(0, 8, 0, 4),
                ToolTip = "Opens the official CLI for separate Code Assist / Workspace quotas." };
            cliLogin.Click += (_, _) => login("gemini-cli");
            details.Children.Add(cliLogin);
            var console = new Button { Content = "Open AI Studio quotas",
                HorizontalAlignment = HorizontalAlignment.Left, Margin = new Thickness(0, 8, 0, 4),
                ToolTip = "API usage is separate from CLI and website quotas. Opens the official API dashboard." };
            console.Click += (_, _) => login(id + "-api");
            details.Children.Add(console);
        }
        var chartHeader = new DockPanel { Margin = new Thickness(0, 12, 0, 8) };
        var range = new ComboBox { ItemsSource = new[] { "1 hour", "6 hours", "24 hours" }, SelectedIndex = 1,
            Width = 112, Foreground = Brush("#F1F1F1"), Background = Brush("#242424"), BorderBrush = Brush("#505050"),
            FontSize = 12, Margin = new Thickness(8, 0, 0, 0) };
        range.SelectionChanged += (_, _) => { chart.Hours = range.SelectedIndex switch { 0 => 1, 2 => 24, _ => 6 }; chart.InvalidateVisual(); };
        DockPanel.SetDock(range, Dock.Right);
        chartHeader.Children.Add(range);
        chartHeader.Children.Add(new TextBlock { Text = "Consumption history", FontWeight = FontWeights.SemiBold,
            VerticalAlignment = VerticalAlignment.Center });
        details.Children.Add(chartHeader);
        details.Children.Add(legend);
        details.Children.Add(chart);
        details.Children.Add(new TextBlock { Text = "Observed quota consumption · This app session · Up to 24 hours",
            Foreground = muted, FontSize = 11, TextWrapping = TextWrapping.Wrap, Margin = new Thickness(0, 4, 0, 0) });
        root.Children.Add(details);
        Child = root;
        Update(UsageSnapshot.Unavailable(id, "Waiting for first refresh…"));
    }

    public void SetLoginEnabled(bool enabled) => signIn.IsEnabled = enabled;

    public void Update(UsageSnapshot snapshot)
    {
        lastSnapshot = snapshot;
        if (snapshot.SignedIn.HasValue) signedIn = snapshot.SignedIn.Value;
        else if (!snapshot.IsCached && snapshot.Quotas.Count > 0 && providerId is not ("cline" or "kilo" or "opencode" or "junie")) signedIn = true;
        signIn.Visibility = signedIn ? Visibility.Collapsed : Visibility.Visible;
        planBadge.Content = (string.IsNullOrWhiteSpace(snapshot.Plan) ? "Plan unavailable" : snapshot.Plan) +
            (details.Visibility == Visibility.Visible ? " ▴" : " ▾");
        planBadge.ToolTip = details.Visibility == Visibility.Visible ? "Hide subscription details and usage history" : "Show subscription details and usage history";
        history.Record(snapshot);
        content.Children.Clear();
        foreach (var quota in snapshot.Quotas)
        {
            var percent = quota.Percent.HasValue ? JsonFields.Clamp(quota.Percent.Value) : (double?)null;
            var remaining = percent.HasValue ? (quota.Consumed ? 100 - percent.Value : percent.Value) : (double?)null;
            var color = Brush((remaining ?? 100) <= 10 ? "#FF737D" : (remaining ?? 100) <= 25 ? "#F4C56A" : "#67DAB1");
            var meter = new Grid { Margin = new Thickness(0, 2, 0, 1) };
            meter.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(122) });
            meter.ColumnDefinitions.Add(new ColumnDefinition());
            meter.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto, MinWidth = 58 });
            meter.Children.Add(new TextBlock { Text = ShortLabel(quota.Title), FontSize = 14,
                Foreground = muted, VerticalAlignment = VerticalAlignment.Center, TextTrimming = TextTrimming.CharacterEllipsis });
            if (percent.HasValue && !quota.Unlimited)
            {
                var bar = new ProgressBar { Minimum = 0, Maximum = 100, Value = percent.Value,
                    Height = 12, Foreground = color, Background = Brush("#45484C"), BorderThickness = new Thickness(0),
                    VerticalAlignment = VerticalAlignment.Center, Margin = new Thickness(0, 0, 12, 0) };
                Grid.SetColumn(bar, 1);
                meter.Children.Add(bar);
            }
            var value = new TextBlock { Text = quota.Unlimited ? "∞" : percent.HasValue ? $"{percent:0}%" : quota.DisplayValue ?? "—",
                Foreground = color, FontWeight = FontWeights.SemiBold, FontSize = 15,
                HorizontalAlignment = HorizontalAlignment.Right, VerticalAlignment = VerticalAlignment.Center };
            Grid.SetColumn(value, 2);
            meter.Children.Add(value);
            content.Children.Add(meter);
            var info = new List<string>();
            if (percent.HasValue && !quota.Unlimited)
                info.Add(quota.Consumed ? $"Used: {percent:0}% / {remaining:0}% available" : $"Remaining: {remaining:0}%");
            if (!string.IsNullOrWhiteSpace(quota.Detail)) info.Add(quota.Detail);
            var resetText = quota.ResetsAt.HasValue ? ResetText(quota.ResetsAt.Value) : "";
            if (info.Count > 0 || resetText.Length > 0)
            {
                var infoGrid = new Grid { Margin = new Thickness(0, 0, 0, 3) };
                infoGrid.ColumnDefinitions.Add(new ColumnDefinition());
                infoGrid.ColumnDefinitions.Add(new ColumnDefinition());
                infoGrid.Children.Add(new TextBlock { Text = string.Join("  /  ", info), Foreground = muted,
                    FontSize = 11, TextWrapping = TextWrapping.Wrap });
                var reset = new TextBlock { Text = resetText, Foreground = muted, FontSize = 11,
                    TextWrapping = TextWrapping.Wrap, HorizontalAlignment = HorizontalAlignment.Right };
                Grid.SetColumn(reset, 1);
                infoGrid.Children.Add(reset);
                content.Children.Add(infoGrid);
            }
        }
        if (!string.IsNullOrWhiteSpace(snapshot.Notice))
            content.Children.Add(new TextBlock { Text = snapshot.Notice, Foreground = muted, TextWrapping = TextWrapping.Wrap });
        summary.Text = providerId == "codex" ? (snapshot.Details ?? []).FirstOrDefault(d => d.StartsWith("Credits:", StringComparison.Ordinal)) ?? "" : "";
        timestamp.Text = snapshot.UpdatedAt.HasValue ? $"Updated {snapshot.UpdatedAt.Value.LocalDateTime:HH:mm}" : "";
        facts.Children.Clear();
        AddFact($"Automatic refresh: every {RefreshSeconds} seconds");
        foreach (var detail in snapshot.Details ?? [])
            if (!(providerId == "codex" && detail.StartsWith("Credits:", StringComparison.Ordinal))) AddFact(detail);
        legend.Children.Clear();
        var names = history.Observations.SelectMany(point => point.Consumed.Keys).Distinct().ToArray();
        for (var i = 0; i < names.Length; i++)
            legend.Children.Add(new TextBlock { Text = names[i].Replace("remaining", "consumed"),
                Foreground = Brush(UsageHistoryChart.SeriesColors[i % UsageHistoryChart.SeriesColors.Length]),
                FontSize = 11, Margin = new Thickness(0, 0, 14, 4) });
        chart.InvalidateVisual();
        ToolTip = providerId == "codex" ? "OpenAI shows remaining quota." : providerId == "copilot" ? "Copilot shows consumed quota: 0% unused, 100% exhausted." : null;
    }

    public void RefreshCountdowns()
    {
        if (lastSnapshot != null && (details.Visibility == Visibility.Visible || lastSnapshot.Quotas.Any(q => q.ResetsAt.HasValue))) Update(lastSnapshot);
    }

    private void AddFact(string text) => facts.Children.Add(new TextBlock { Text = text, Foreground = muted,
        FontSize = 12, TextWrapping = TextWrapping.Wrap, Margin = new Thickness(0, 0, 0, 5) });

    private static string ResetText(DateTimeOffset reset)
    {
        var duration = reset - DateTimeOffset.Now;
        var countdown = duration <= TimeSpan.Zero ? "reset due" : duration.TotalDays >= 1
            ? $"{(int)duration.TotalDays}d {duration.Hours}h" : duration.TotalHours >= 1
                ? $"{(int)duration.TotalHours}h {duration.Minutes}m" : $"{Math.Max(1, duration.Minutes)}m";
        return $"Resets {reset.LocalDateTime:g} ({countdown})";
    }
    private static string PlanSurface(string id) => id switch
    {
        "codex" => "#17483E",
        "copilot" => "#193A5E",
        "claude" => "#503A2B",
        "gemini" => "#2C3F70",
        _ => "#3C4148"
    };

    private static string ShortLabel(string title) => title switch
    {
        "5-hour remaining" => "5 hours",
        "Weekly remaining" => "This week",
        "Premium requests used" => "Premium",
        "AI credits used" => "AI credits",
        "Completions used" => "Completions",
        "Chat used" => "Chat",
        "Top-up" => "Top-up",
        "AI credits" => "AI credits",
        "Completions" => "Completions",
        _ => title
    };
    private static SolidColorBrush Brush(string hex)
    {
        var brush = new SolidColorBrush((Color)ColorConverter.ConvertFromString(hex));
        brush.Freeze();
        return brush;
    }
}
