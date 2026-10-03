using System;
using System.ComponentModel.Design;
using System.Runtime.InteropServices;
using System.Threading;
using System.Threading.Tasks;
using AgentsUsage.VisualStudio.Settings;
using AgentsUsage.VisualStudio.UI;
using Microsoft.VisualStudio.Shell;

namespace AgentsUsage.VisualStudio
{
    [PackageRegistration(UseManagedResourcesOnly = true, AllowsBackgroundLoading = true)]
    [ProvideMenuResource("Menus.ctmenu", 1)]
    [ProvideToolWindow(typeof(UsageToolWindow))]
    [ProvideOptionPage(typeof(UsageOptions), "Agents Usage", "General", 0, 0, true)]
    [Guid(PackageId)]
    public sealed class AgentsUsagePackage : AsyncPackage
    {
        public const string PackageId = "6f46d6f4-1fae-4901-ab01-c8c935061634";
        internal static readonly Guid CommandSet = new Guid("b2211322-785e-4478-a789-ae9b64034639");

        protected override async Task InitializeAsync(CancellationToken cancellationToken, IProgress<ServiceProgressData> progress)
        {
            await JoinableTaskFactory.SwitchToMainThreadAsync(cancellationToken);
            if (await GetServiceAsync(typeof(IMenuCommandService)) is OleMenuCommandService commands)
            {
                commands.AddCommand(new MenuCommand((sender, args) =>
                {
                    JoinableTaskFactory.RunAsync(async () =>
                    {
                        await ShowToolWindowAsync(typeof(UsageToolWindow), 0, true, DisposalToken);
                    }).FileAndForget("AgentsUsage/OpenUsage");
                }, new CommandID(CommandSet, 0x0100)));
            }
        }

        internal UsageOptions Options => (UsageOptions)GetDialogPage(typeof(UsageOptions));
        internal void OpenSettings() => ShowOptionPage(typeof(UsageOptions));
    }
}
