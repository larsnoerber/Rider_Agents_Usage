using System.Globalization;
using System.Windows;
using System.Windows.Media;
using AgentMeter.Windows.Core.History;

namespace AgentMeter.Windows.UI.Components;

internal sealed class UsageHistoryChart : FrameworkElement
{
    internal static readonly string[] SeriesColors = ["#BD9AFF", "#84B8FF", "#67DAB1", "#F3B38D", "#F4C56A"];
    public UsageHistory History { get; }
    public int Hours { get; set; } = 6;

    public UsageHistoryChart(UsageHistory history) { History = history; Height = 180; MinWidth = 200; }

    protected override void OnRender(DrawingContext dc)
    {
        base.OnRender(dc);
        var end = DateTimeOffset.Now;
        var start = end.AddHours(-Hours);
        var points = History.Observations.Where(point => point.At >= start && point.At <= end).ToArray();
        var left = 38d;
        var top = 10d;
        var width = Math.Max(1, ActualWidth - left - 12);
        var height = Math.Max(1, ActualHeight - top - 32);
        var muted = Brush("#9BAAC1");
        var grid = new Pen(Brush("#2A3A55"), 1);
        for (var percent = 0; percent <= 100; percent += 25)
        {
            var y = top + height * (1 - percent / 100d);
            dc.DrawLine(grid, new Point(left, y), new Point(left + width, y));
            Label(dc, percent.ToString(CultureInfo.InvariantCulture), 0, y - 7, muted);
        }
        for (var tick = 0; tick <= 3; tick++)
        {
            var time = start.AddHours(Hours * tick / 3d);
            var x = left + width * tick / 3;
            Label(dc, time.LocalDateTime.ToString("HH:mm"), Math.Min(x - 14, ActualWidth - 34), top + height + 6, muted);
        }
        if (points.Length == 0)
        {
            Label(dc, "History appears after a quota refresh", left + 12, top + height / 2 - 7, muted);
            return;
        }
        var names = History.Observations.SelectMany(point => point.Consumed.Keys).Distinct().ToArray();
        for (var series = 0; series < names.Length; series++)
        {
            var color = Brush(SeriesColors[series % SeriesColors.Length]);
            Point? previous = null;
            foreach (var point in points)
            {
                if (!point.Consumed.TryGetValue(names[series], out var value)) { previous = null; continue; }
                var current = new Point(left + width * (point.At - start).TotalHours / Hours, top + height * (1 - value / 100));
                if (previous is Point before) dc.DrawLine(new Pen(color, 2), before, current);
                dc.DrawEllipse(color, null, current, 2.5, 2.5);
                previous = current;
            }
        }
        dc.DrawLine(grid, new Point(left, top), new Point(left, top + height));
    }

    private void Label(DrawingContext dc, string text, double x, double y, Brush brush) =>
        dc.DrawText(new FormattedText(text, CultureInfo.CurrentCulture, FlowDirection.LeftToRight,
            new Typeface("Segoe UI"), 11, brush, VisualTreeHelper.GetDpi(this).PixelsPerDip), new Point(x, y));

    private static SolidColorBrush Brush(string color) => new((Color)ColorConverter.ConvertFromString(color));
}
