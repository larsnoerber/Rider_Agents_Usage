package io.github.larsnoerber.agentsusage.providers.copilot

data class CopilotQuota(
    val title: String,
    val percentLeft: Int?,
    val remaining: Int? = null,
    val total: Int? = null,
    val unlimited: Boolean = false
) {
    val displayRemaining: String
        get() = when {
            unlimited -> "Unlimited"
            remaining != null && total != null -> "$remaining / $total"
            percentLeft != null -> "$percentLeft%"
            else -> "—"
        }
}

data class GitHubCopilotUsage(
    val primary: CopilotQuota? = null,
    val chat: CopilotQuota? = null,
    val completions: CopilotQuota? = null,
    val plan: String? = null,
    val resetsAt: Long? = null,
    val reportedAt: Long? = null,
    val error: String? = null
)
