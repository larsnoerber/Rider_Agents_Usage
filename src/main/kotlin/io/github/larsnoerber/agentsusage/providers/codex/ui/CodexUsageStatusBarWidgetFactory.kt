package io.github.larsnoerber.agentsusage.providers.codex.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import io.github.larsnoerber.agentsusage.core.format.formatSubscriptionPlan
import io.github.larsnoerber.agentsusage.providers.codex.CodexUsage
import io.github.larsnoerber.agentsusage.providers.codex.CodexUsageService
import io.github.larsnoerber.agentsusage.ui.components.StatusBarPart
import io.github.larsnoerber.agentsusage.ui.components.StatusBarPresentation
import io.github.larsnoerber.agentsusage.ui.components.UsageStatusBarWidget
import io.github.larsnoerber.agentsusage.ui.components.usageBarColor
import io.github.larsnoerber.agentsusage.ui.components.usageTooltip

class CodexUsageStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = "CodexUsageStatusBar"
    override fun getDisplayName(): String = "OpenAI Usage"
    override fun isAvailable(project: Project): Boolean = true
    override fun createWidget(project: Project): StatusBarWidget =
        UsageStatusBarWidget(project, getId(), CodexUsageService.getInstance(), ::codexPresentation)
    override fun disposeWidget(widget: StatusBarWidget) = widget.dispose()
}

private fun codexPresentation(usage: CodexUsage): StatusBarPresentation {
    val five = usage.fiveHourLeft
    val week = usage.weeklyLeft
    val parts = if (five == null || week == null) {
        listOf(StatusBarPart(if (usage.error == null) "OpenAI …" else "OpenAI —", usageBarColor(null)))
    } else {
        listOf(
            StatusBarPart("OpenAI 5h $five%", usageBarColor(five)),
            StatusBarPart("W $week%${if (usage.error == null) "" else " !"}", usageBarColor(week))
        )
    }
    return StatusBarPresentation(
        parts = parts,
        dimmed = five == null || week == null,
        tooltip = usageTooltip("OpenAI Codex usage", listOf(
            "Subscription: ${formatSubscriptionPlan(usage.plan)}",
            "5h: remaining ${five ?: "—"}% · ${usage.fiveHourReset ?: "reset time unknown"}",
            "Weekly: remaining ${week ?: "—"}% · ${usage.weeklyReset ?: "reset time unknown"}",
            "Credits: ${usage.credits ?: "—"}",
            usage.error ?: "Click to open Agents Usage and view details"
        ))
    )
}
