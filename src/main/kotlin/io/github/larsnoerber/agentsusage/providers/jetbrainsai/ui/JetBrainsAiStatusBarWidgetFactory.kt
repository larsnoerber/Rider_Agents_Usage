package io.github.larsnoerber.agentsusage.providers.jetbrainsai.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import io.github.larsnoerber.agentsusage.core.format.formatResetTime
import io.github.larsnoerber.agentsusage.core.format.formatSubscriptionPlan
import io.github.larsnoerber.agentsusage.providers.jetbrainsai.JetBrainsAiUsage
import io.github.larsnoerber.agentsusage.providers.jetbrainsai.JetBrainsAiUsageService
import io.github.larsnoerber.agentsusage.providers.jetbrainsai.formatAiCredits
import io.github.larsnoerber.agentsusage.ui.components.StatusBarPart
import io.github.larsnoerber.agentsusage.ui.components.StatusBarPresentation
import io.github.larsnoerber.agentsusage.ui.components.UsageStatusBarWidget
import io.github.larsnoerber.agentsusage.ui.components.usageBarColor
import io.github.larsnoerber.agentsusage.ui.components.usageTooltip
import java.awt.Color

class JetBrainsAiStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = "JetBrainsAiCreditsStatusBar"
    override fun getDisplayName(): String = "JetBrains AI Credits"
    override fun isAvailable(project: Project): Boolean = JetBrainsAiUsageService.isAvailable()
    override fun createWidget(project: Project): StatusBarWidget =
        UsageStatusBarWidget(project, getId(), JetBrainsAiUsageService.getInstance(), ::jetBrainsPresentation)
    override fun disposeWidget(widget: StatusBarWidget) = widget.dispose()
}

private fun jetBrainsPresentation(usage: JetBrainsAiUsage): StatusBarPresentation {
    val text = when {
        usage.unlimited -> "JB AI ∞"
        usage.quota != null -> "JB AI ${formatAiCredits(usage.quota.remaining)} cr"
        usage.error != null -> "JB AI —"
        else -> "JB AI …"
    }
    val lines = buildList {
        add("Subscription: ${formatSubscriptionPlan(usage.plan)}")
        usage.quota?.let { add("Remaining: ${formatAiCredits(it.remaining)} / ${formatAiCredits(it.total)} credits") }
        usage.subscription?.let { add("Subscription: ${formatAiCredits(it.remaining)} / ${formatAiCredits(it.total)} credits") }
        usage.topUp?.let { add("Top-up: ${formatAiCredits(it.remaining)} / ${formatAiCredits(it.total)} credits") }
        usage.resetsAt?.let { add("Subscription resets: ${formatResetTime(it)}") }
        if (usage.unlimited) add("Unlimited quota")
        add(usage.error ?: "Click to open usage details")
    }
    return StatusBarPresentation(
        listOf(StatusBarPart(text, aiCreditColor(usage))),
        usageTooltip("JetBrains AI credits", lines),
        dimmed = usage.error != null
    )
}

internal fun aiCreditColor(usage: JetBrainsAiUsage): Color = usageBarColor(
    if (usage.error != null) null else if (usage.unlimited) 100 else usage.quota?.percentLeft
)
