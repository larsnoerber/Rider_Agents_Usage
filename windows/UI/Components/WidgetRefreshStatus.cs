using System.Windows;
using System.Windows.Controls;

namespace AgentMeter.Windows.UI.Components;

internal sealed class WidgetRefreshStatus : StackPanel
{
    private readonly TextBlock label = new() { FontSize = 12, Margin = new Thickness(0, 0, 0, 5) };
    private readonly ProgressBar progress = new() { Height = 3, Minimum = 0, BorderThickness = new Thickness(0) };

    public WidgetRefreshStatus()
    {
        MinWidth = 210;
        Margin = new Thickness(2, 3, 2, 3);
        VerticalAlignment = VerticalAlignment.Center;
        Children.Add(label);
        Children.Add(progress);
        System.Windows.Automation.AutomationProperties.SetName(this, "Provider refresh progress");
    }

    public void Update(int completed, int total, WidgetColorChoice colors)
    {
        label.Text = $"Updating providers… {completed}/{total}";
        label.Foreground = colors.TextBrush;
        progress.Foreground = colors.AccentBrush;
        progress.Background = colors.TrackBrush;
        progress.Maximum = Math.Max(1, total);
        progress.Value = completed;
        progress.IsIndeterminate = completed == 0;
    }
}
