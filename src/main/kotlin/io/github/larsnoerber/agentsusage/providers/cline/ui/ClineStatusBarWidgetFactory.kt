package io.github.larsnoerber.agentsusage.providers.cline.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import io.github.larsnoerber.agentsusage.providers.cline.ClineUsage
import io.github.larsnoerber.agentsusage.providers.cline.ClineUsageService
import io.github.larsnoerber.agentsusage.settings.AgentsUsageSettings
import io.github.larsnoerber.agentsusage.ui.components.*

class ClineStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = "ClineUsageStatusBar"
    override fun getDisplayName(): String = "Cline Usage"
    override fun isAvailable(project: Project): Boolean =
        AgentsUsageSettings.getInstance().state.showCline && ClineUsageService.isAvailable()
    override fun createWidget(project: Project): StatusBarWidget =
        UsageStatusBarWidget(project, getId(), ClineUsageService.getInstance(), ::presentation)
    override fun disposeWidget(widget: StatusBarWidget) = widget.dispose()
}

private fun presentation(usage: ClineUsage): StatusBarPresentation = StatusBarPresentation(
    listOf(StatusBarPart("Cline | "), StatusBarPart(if (usage.accountBalanceUsd != null)
        "$%.2f".format(java.util.Locale.US, usage.accountBalanceUsd)
        else if (usage.sources > 0) "%,d tokens".format(usage.totalTokens)
        else if (usage.error == null) "…" else "—", secondaryTextColor())),
    usageTooltip("Cline local usage", listOf("Tasks: ${usage.tasks}", "Input: ${usage.inputTokens}",
        "Output: ${usage.outputTokens}", "Reported cost: ${usage.costUsd?.let { "$%.2f".format(java.util.Locale.US, it) } ?: "—"}",
        "Account balance: ${usage.accountBalanceUsd?.let { "$%.2f".format(java.util.Locale.US, it) } ?: "—"}",
        "All recorded local tasks; Cline account uses pay-as-you-go credits",
        usage.accountError ?: if (usage.accountBalanceUsd != null) "Account connected" else "Account balance loading",
        usage.error ?: "Click to open usage details")),
    dimmed = usage.error != null && usage.accountBalanceUsd == null
)
