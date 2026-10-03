using System.Windows.Controls;
using AgentsUsage.VisualStudio.UI.Components;

namespace AgentsUsage.VisualStudio.Providers.Codex.UI
{
    internal static class CodexUsagePanel
    {
        public static Border Create(CodexUsage usage)
        {
            var card = new StackPanel();
            card.Children.Add(QuotaElements.Label("OpenAI Codex", 15, true));
            card.Children.Add(QuotaElements.Label("Plan: " + (string.IsNullOrWhiteSpace(usage?.Plan) ? "Unknown" : usage.Plan)));
            if (usage == null) card.Children.Add(QuotaElements.Label("Waiting for provider quota…"));
            else
            {
                QuotaElements.AddQuota(card, "5-hour quota", usage.FiveHourLeft, usage.FiveHourReset, true);
                QuotaElements.AddQuota(card, "Weekly quota", usage.WeeklyLeft, usage.WeeklyReset, true);
                if (usage.Credits.HasValue) card.Children.Add(QuotaElements.Label("Credits: " + usage.Credits.Value.ToString("N2")));
                if (usage.Error != null) card.Children.Add(QuotaElements.Label(usage.Error));
                card.Children.Add(QuotaElements.Label("Updated: " + usage.UpdatedAt.ToLocalTime().ToString("g"), 11));
            }
            return QuotaElements.Card(card);
        }
    }
}
