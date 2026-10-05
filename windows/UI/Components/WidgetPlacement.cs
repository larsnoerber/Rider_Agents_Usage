using System.Windows;
using System.Windows.Interop;
using System.Windows.Media;

namespace AgentMeter.Windows.UI.Components;

internal static class WidgetPlacement
{
    internal static Rect WorkArea(Window window)
    {
        var source = PresentationSource.FromVisual(window) as HwndSource;
        if (source == null) return SystemParameters.WorkArea;
        var area = System.Windows.Forms.Screen.FromHandle(source.Handle).WorkingArea;
        var matrix = source.CompositionTarget.TransformFromDevice;
        return new Rect(matrix.Transform(new Point(area.Left, area.Top)),
            matrix.Transform(new Point(area.Right, area.Bottom)));
    }

    internal static void Constrain(Window window, bool snap)
    {
        var area = WorkArea(window);
        var right = Math.Max(area.Left, area.Right - window.ActualWidth);
        var bottom = Math.Max(area.Top, area.Bottom - window.ActualHeight);
        var left = Math.Clamp(window.Left, area.Left, right);
        var top = Math.Clamp(window.Top, area.Top, bottom);
        if (snap)
        {
            if (Math.Abs(left - area.Left) <= 18) left = area.Left;
            else if (Math.Abs(left - right) <= 18) left = right;
            if (Math.Abs(top - area.Top) <= 18) top = area.Top;
            else if (Math.Abs(top - bottom) <= 18) top = bottom;
        }
        window.Left = left;
        window.Top = top;
    }
}
