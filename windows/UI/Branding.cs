using System.Drawing;
using System.Windows.Media.Imaging;

namespace AgentMeter.Windows.UI;

internal static class Branding
{
    // Tray and WPF windows use frames from the same generated SVG-backed ICO resource.
    public static Icon TrayIcon()
    {
        using var stream = typeof(Branding).Assembly.GetManifestResourceStream("AgentMeter.Windows.Logo.ico")
            ?? throw new InvalidOperationException("AgentMeter logo is missing.");
        using var source = new Icon(stream, 32, 32);
        return (Icon)source.Clone();
    }

    public static BitmapSource WindowIcon()
    {
        using var stream = typeof(Branding).Assembly.GetManifestResourceStream("AgentMeter.Windows.Logo.ico")
            ?? throw new InvalidOperationException("AgentMeter logo is missing.");
        var bitmap = BitmapDecoder.Create(stream, BitmapCreateOptions.None, BitmapCacheOption.OnLoad)
            .Frames.OrderByDescending(frame => frame.PixelWidth).First();
        bitmap.Freeze();
        return bitmap;
    }
}
