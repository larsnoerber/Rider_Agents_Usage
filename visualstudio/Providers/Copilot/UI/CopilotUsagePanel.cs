using System.Windows.Controls;
using AgentsUsage.VisualStudio.UI.Components;

namespace AgentsUsage.VisualStudio.Providers.Copilot.UI
{
    internal static class CopilotUsagePanel
    {
        public static Border Create(CopilotUsage usage)
        {
            var card = new StackPanel();
            card.Children.Add(QuotaElements.Label("GitHub Copilot", 15, true));
            card.Children.Add(QuotaElements.Label("Plan: " + (string.IsNullOrWhiteSpace(usage?.Plan) ? "Unknown" : usage.Plan)));
            if (usage == null) card.Children.Add(QuotaElements.Label("Waiting for provider quota…"));
            else
            {
                foreach (var quota in usage.Quotas)
                {
                    QuotaElements.AddQuota(card, quota.Title, quota.PercentUsed, quota.Reset, false, quota.Unlimited);
                    if (!quota.TokenBilling && !quota.Unlimited && quota.Used.HasValue && quota.Limit > 0)
                        card.Children.Add(QuotaElements.Label("Used: " + quota.Used.Value.ToString("N0") + " / " + quota.Limit.Value.ToString("N0"), 11));
                }
                if (usage.Error != null) card.Children.Add(QuotaElements.Label(usage.Error));
                card.Children.Add(QuotaElements.Label("Updated: " + usage.UpdatedAt.ToLocalTime().ToString("g"), 11));
            }
            return QuotaElements.Card(card);
        }
    }
}
