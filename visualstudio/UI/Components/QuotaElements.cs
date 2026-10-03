using System;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using Microsoft.VisualStudio.PlatformUI;

namespace AgentsUsage.VisualStudio.UI.Components
{
    internal static class QuotaElements
    {
        private static readonly Brush Green = FrozenBrush(100, 205, 135);
        private static readonly Brush Amber = FrozenBrush(255, 185, 75);
        private static readonly Brush Red = FrozenBrush(255, 110, 110);
        public static Brush Color(int value, bool remaining) => (remaining ? 100 - value : value) < 50 ? Green
            : (remaining ? 100 - value : value) < 80 ? Amber : Red;

        private static Brush FrozenBrush(byte red, byte green, byte blue)
        {
            var brush = new SolidColorBrush(System.Windows.Media.Color.FromRgb(red, green, blue));
            brush.Freeze();
            return brush;
        }

        public static Image Icon(double size) => new Image
        {
            Source = new BitmapImage(new Uri("pack://application:,,,/AgentsUsage.VisualStudio;component/Resources/icon.png")),
            Width = size, Height = size, Margin = new Thickness(0, 0, 8, 0)
        };

        public static TextBlock Label(string text, double size = 12, bool bold = false) => new TextBlock
        {
            Text = text, FontSize = size, FontWeight = bold ? FontWeights.SemiBold : FontWeights.Normal,
            TextWrapping = TextWrapping.Wrap, Margin = new Thickness(0, 2, 0, 2)
        };

        public static Border Card(StackPanel panel)
        {
            panel.Margin = new Thickness(12);
            var surface = new Border { Child = panel, BorderThickness = new Thickness(1), CornerRadius = new CornerRadius(6), Margin = new Thickness(0, 0, 0, 12) };
            surface.SetResourceReference(Border.BorderBrushProperty, EnvironmentColors.ToolWindowTextBrushKey);
            return surface;
        }

        public static void AddQuota(Panel panel, string title, int? percentage, DateTimeOffset? reset, bool remaining, bool unlimited = false)
        {
            var row = new DockPanel { Margin = new Thickness(0, 14, 0, 6) };
            var value = Label(unlimited ? "Unlimited" : percentage.HasValue ? percentage.Value + (remaining ? "% remaining" : "% used") : "Unavailable", 13, true);
            if (percentage.HasValue) value.Foreground = Color(percentage.Value, remaining);
            DockPanel.SetDock(value, Dock.Right);
            row.Children.Add(value);
            row.Children.Add(Label(title, 13));
            panel.Children.Add(row);
            if (percentage.HasValue && !unlimited) panel.Children.Add(new ProgressBar
            {
                Minimum = 0, Maximum = 100, Value = percentage.Value, Height = 7, Foreground = Color(percentage.Value, remaining)
            });
            if (reset.HasValue) panel.Children.Add(Label("Resets: " + reset.Value.ToLocalTime().ToString("g"), 11));
        }
    }
}
