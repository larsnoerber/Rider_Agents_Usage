using System;
using System.ComponentModel.Design;
using System.Runtime.InteropServices;
using System.Threading;
using System.Threading.Tasks;
using AgentsUsage.VisualStudio.Settings;
using AgentsUsage.VisualStudio.UI;
using AgentsUsage.VisualStudio.Application;
using Microsoft.VisualStudio.Shell.ServiceBroker;
using Microsoft.VisualStudio.Shell;
using Microsoft.VisualStudio.Shell.Interop;

namespace AgentsUsage.VisualStudio
{
    [PackageRegistration(UseManagedResourcesOnly = true, AllowsBackgroundLoading = true)]
    [ProvideMenuResource("Menus.ctmenu", 1)]
    [ProvideToolWindow(typeof(UsageToolWindow))]
    [ProvideOptionPage(typeof(UsageOptions), "AgentMeter", "General", 0, 0, true)]
    [ProvideAutoLoad(UIContextGuids80.NoSolution, PackageAutoLoadFlags.BackgroundLoad)]
    [ProvideAutoLoad(UIContextGuids80.SolutionExists, PackageAutoLoadFlags.BackgroundLoad)]
    [Guid(PackageId)]
    public sealed class AgentsUsagePackage : AsyncPackage
    {
        public const string PackageId = "6f46d6f4-1fae-4901-ab01-c8c935061634";
        internal static readonly Guid CommandSet = new Guid("b2211322-785e-4478-a789-ae9b64034639");
        internal UsageRefreshCoordinator Coordinator { get; private set; }
        private StatusBarHost statusBar;

        protected override async Task InitializeAsync(CancellationToken cancellationToken, IProgress<ServiceProgressData> progress)
        {
            var container = await GetServiceAsync(typeof(SVsBrokeredServiceContainer)) as IBrokeredServiceContainer;
            await JoinableTaskFactory.SwitchToMainThreadAsync(cancellationToken);
            Coordinator = new UsageRefreshCoordinator(Options, container?.GetFullAccessServiceBroker());
            statusBar = new StatusBarHost(Options, Coordinator, OpenUsage);
            if (await GetServiceAsync(typeof(IMenuCommandService)) is OleMenuCommandService commands)
            {
                commands.AddCommand(new MenuCommand((sender, args) => OpenUsage(), new CommandID(CommandSet, 0x0100)));
            }
            _ = Coordinator.RefreshAsync();
        }

        internal void OpenUsage() => JoinableTaskFactory.RunAsync(async () =>
        {
            await ShowToolWindowAsync(typeof(UsageToolWindow), 0, true, DisposalToken);
        }).FileAndForget("AgentsUsage/OpenUsage");

        protected override void Dispose(bool disposing)
        {
            if (disposing)
            {
                JoinableTaskFactory.Run(async () =>
                {
                    await JoinableTaskFactory.SwitchToMainThreadAsync();
                    statusBar?.Dispose();
                    Coordinator?.Dispose();
                });
            }
            base.Dispose(disposing);
        }

        internal UsageOptions Options => (UsageOptions)GetDialogPage(typeof(UsageOptions));
        internal void OpenSettings() => ShowOptionPage(typeof(UsageOptions));
    }
}
