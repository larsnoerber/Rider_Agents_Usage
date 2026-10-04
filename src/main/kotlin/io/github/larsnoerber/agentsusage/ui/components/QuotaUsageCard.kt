package io.github.larsnoerber.agentsusage.ui.components

import io.github.larsnoerber.agentsusage.core.format.formatResetTime

internal class QuotaUsageCard(title: String, private val showRemaining: Boolean = false) : UsageCard(title) {
    fun render(percentUsed: Int?, resetsAt: Long?) {
        val percent = percentUsed?.let { if (showRemaining) 100 - it else it }
        remaining.text = percent?.let { "$it%" } ?: "—"
        detail.text = if (percent != null) "$percent% ${if (showRemaining) "remaining" else "consumed"}" else "Quota unavailable"
        updateProgress(percent, usageBarColor(percentUsed?.let { 100 - it }))
        updateDetailsTooltip(resetsAt?.let { "Resets: ${formatResetTime(it)}" })
    }
}
