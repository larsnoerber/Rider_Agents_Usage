using System.Drawing;
using System.Windows.Threading;
using Forms = System.Windows.Forms;

namespace AgentMeter.Windows.UI;

internal sealed class TrayHost : IDisposable
{
    private readonly Forms.NotifyIcon notification;
    private readonly Forms.ContextMenuStrip menu;
    private readonly Icon logo;
    private bool disposed;

    public TrayHost(Dispatcher dispatcher, Action openDashboard, Action desktopWidget, Action refresh, Action exit)
    {
        logo = Branding.TrayIcon();
        menu = new Forms.ContextMenuStrip();
        Add("Open dashboard", openDashboard);
        Add("Show desktop widget", desktopWidget);
        Add("Refresh all", refresh);
        menu.Items.Add(new Forms.ToolStripSeparator());
        Add("Exit AgentMeter", exit);
        notification = new Forms.NotifyIcon { Icon = logo, Text = "AgentMeter", ContextMenuStrip = menu, Visible = true };
        notification.DoubleClick += Open;

        void Dispatch(Action action)
        {
            if (!disposed && !dispatcher.HasShutdownStarted)
                dispatcher.BeginInvoke(() => { if (!disposed) action(); });
        }
        void Add(string title, Action action) => menu.Items.Add(title, null, (_, _) => Dispatch(action));
        void Open(object? sender, EventArgs args) => Dispatch(openDashboard);
    }

    public void Dispose()
    {
        if (disposed) return;
        disposed = true;
        notification.Visible = false;
        notification.Dispose();
        menu.Dispose();
        logo.Dispose();
    }
}
