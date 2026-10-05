using System;

namespace AgentsUsage.VisualStudio.Providers.Codex
{
    internal sealed class CodexUsage
    {
        public int? FiveHourLeft { get; set; }
        public int? WeeklyLeft { get; set; }
        public DateTimeOffset? FiveHourReset { get; set; }
        public DateTimeOffset? WeeklyReset { get; set; }
        public double? Credits { get; set; }
        public string Plan { get; set; }
        public string Error { get; set; }
        public bool? SignedIn { get; set; }
        public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.Now;
    }
}
