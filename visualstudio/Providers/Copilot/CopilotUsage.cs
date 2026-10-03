using System;
using System.Collections.Generic;

namespace AgentsUsage.VisualStudio.Providers.Copilot
{
    internal sealed class CopilotUsage
    {
        public string Plan { get; set; }
        public string Error { get; set; }
        public CopilotQuota Primary { get; set; }
        public IReadOnlyList<CopilotQuota> Quotas { get; set; } = Array.Empty<CopilotQuota>();
        public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.Now;
    }

    internal sealed class CopilotQuota
    {
        public string Category { get; set; }
        public string Title { get; set; }
        public int? PercentUsed { get; set; }
        public bool Unlimited { get; set; }
        public bool TokenBilling { get; set; }
        public double? Used { get; set; }
        public double? Limit { get; set; }
        public DateTimeOffset? Reset { get; set; }
    }
}
