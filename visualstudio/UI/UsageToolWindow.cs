using System;
using System.Runtime.InteropServices;
using System.Windows.Controls;
using Microsoft.VisualStudio.Shell;

namespace AgentsUsage.VisualStudio.UI
{
    [Guid("14b3ce44-b189-4167-a11f-65a8d971b3f3")]
    public sealed class UsageToolWindow : ToolWindowPane
    {
        private UsageControl control;
        private readonly ContentControl host = new ContentControl();

        public UsageToolWindow() : base(null) { Caption = "Agents Usage"; Content = host; }

        public override void OnToolWindowCreated()
        {
            base.OnToolWindowCreated();
            if (control != null) return;
            var package = (AgentsUsagePackage)Package;
            control = new UsageControl(package.Options, package.OpenSettings);
            host.Content = control;
        }

        protected override void Dispose(bool disposing)
        {
            if (disposing) control?.Dispose();
            base.Dispose(disposing);
        }
    }
}
