using System.Globalization;
using System.Windows;
using System.Windows.Media;
using AgentMeter.Windows.Core;

namespace AgentMeter.Windows.UI.Components;

internal sealed class QuotaRing(UsageQuota? quota, WidgetColorChoice colors) : FrameworkElement
{
    public static Brush QuotaBrush(UsageQuota? quota, WidgetColorChoice? colors = null)
    {
        var remaining = quota?.Percent is double value ? quota.Consumed ? 100 - value : value : (double?)null;
        return new SolidColorBrush((Color)ColorConverter.ConvertFromString(remaining == null ? "#9BAAC1"
            : remaining > 25 ? (colors ?? WidgetColors.Choices[0]).Accent : remaining <= 10 ? "#FF737D" : remaining <= 25 ? "#F4C56A" : "#67DAB1"));
    }

    protected override void OnRender(DrawingContext drawing)
    {
        base.OnRender(drawing);
        var center = new Point(ActualWidth / 2, ActualHeight / 2);
        var radius = Math.Max(0, Math.Min(ActualWidth, ActualHeight) / 2 - 3);
        var color = QuotaBrush(quota, colors);
        var stroke = new Pen(color, 3) { StartLineCap = PenLineCap.Round, EndLineCap = PenLineCap.Round };
        drawing.DrawEllipse(null, new Pen(colors.TrackBrush, 3), center, radius, radius);
        if (quota?.Percent is double value && !quota.Unlimited)
        {
            var percent = JsonFields.Clamp(value);
            if (percent >= 100) drawing.DrawEllipse(null, stroke, center, radius, radius);
            else if (percent > 0)
            {
                var angle = percent / 100 * Math.PI * 2 - Math.PI / 2;
                var geometry = new StreamGeometry();
                using (var context = geometry.Open())
                {
                    context.BeginFigure(new Point(center.X, center.Y - radius), false, false);
                    context.ArcTo(new Point(center.X + radius * Math.Cos(angle), center.Y + radius * Math.Sin(angle)),
                        new Size(radius, radius), 0, percent > 50, SweepDirection.Clockwise, true, false);
                }
                geometry.Freeze();
                drawing.DrawGeometry(null, stroke, geometry);
            }
        }
        var label = quota?.Unlimited == true ? "∞" : quota?.Percent is double p ? $"{p:0}%" : quota?.DisplayValue ?? "—";
        var text = new FormattedText(label, CultureInfo.CurrentCulture, FlowDirection.LeftToRight,
            new Typeface(new FontFamily("Segoe UI"), FontStyles.Normal, FontWeights.SemiBold, FontStretches.Normal),
            12, color, VisualTreeHelper.GetDpi(this).PixelsPerDip);
        if (text.Width > ActualWidth - 6) text.SetFontSize(Math.Max(7, 12 * (ActualWidth - 6) / text.Width));
        drawing.DrawText(text, new Point(center.X - text.Width / 2, center.Y - text.Height / 2));
    }
}
