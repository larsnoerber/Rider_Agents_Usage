using System;
using System.Runtime.InteropServices;
using System.Windows.Controls;
using Microsoft.VisualStudio.Shell;
using Microsoft.VisualStudio.Imaging.Interop;

namespace AgentsUsage.VisualStudio.UI
{
    [Guid("14b3ce44-b189-4167-a11f-65a8d971b3f3")]
    public sealed class UsageToolWindow : ToolWindowPane
    {
        private UsageControl control;
        private readonly ContentControl host = new ContentControl();

        public UsageToolWindow() : base(null)
        {
            Caption = "AgentMeter";
            Content = host;
            BitmapImageMoniker = new ImageMoniker { Guid = new Guid("988815e6-cdbc-44b5-a9ae-b81ee39b3975"), Id = 1 };
        }

        public override void OnToolWindowCreated()
        {
            base.OnToolWindowCreated();
            if (control != null) return;
            var package = (AgentsUsagePackage)Package;
            control = new UsageControl(package.Options, package.Coordinator, package.OpenSettings);
            host.Content = control;
        }

        protected override void Dispose(bool disposing)
        {
            if (disposing) control?.Dispose();
            base.Dispose(disposing);
        }
    }
}
