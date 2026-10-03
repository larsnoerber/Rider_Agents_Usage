package io.github.larsnoerber.agentsusage.providers.codex.ui

import io.github.larsnoerber.agentsusage.core.format.resetCountdown
import io.github.larsnoerber.agentsusage.ui.components.UsageCard
import io.github.larsnoerber.agentsusage.ui.components.usageBarColor

internal class CodexUsageCard(title: String) : UsageCard(title) {
    private var resetsAt: Long? = null

    fun render(left: Int?, displayedReset: String?, epochSeconds: Long?) {
        remaining.text = left?.let { "$it%" } ?: "—"
        detail.text = displayedReset ?: "Reset time unknown"
        resetsAt = epochSeconds
        updateProgress(left, usageBarColor(left))
        updateCountdown()
    }

    fun updateCountdown() {
        updateDetailsTooltip(resetsAt?.let(::resetCountdown))
    }
}
