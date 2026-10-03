package io.github.larsnoerber.agentsusage.providers.jetbrainsai.ui

import com.intellij.openapi.Disposable
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import io.github.larsnoerber.agentsusage.core.format.formatResetTime
import io.github.larsnoerber.agentsusage.providers.jetbrainsai.JetBrainsAiQuota
import io.github.larsnoerber.agentsusage.providers.jetbrainsai.JetBrainsAiUsage
import io.github.larsnoerber.agentsusage.providers.jetbrainsai.JetBrainsAiUsageService
import io.github.larsnoerber.agentsusage.providers.jetbrainsai.formatAiCredits
import io.github.larsnoerber.agentsusage.ui.components.ProviderHeader
import io.github.larsnoerber.agentsusage.ui.components.UsageCard
import io.github.larsnoerber.agentsusage.ui.components.compactUsageMessage
import io.github.larsnoerber.agentsusage.ui.components.usageBarColor
import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.JPanel

internal class JetBrainsAiUsagePanel : JPanel(BorderLayout(0, 5)), Disposable {
    private val header = ProviderHeader("JetBrains AI")
    private val subscription = AiCreditCard("Subscription")
    private val topUp = AiCreditCard("Top-up")
    private val status = JBLabel().apply { foreground = JBColor.GRAY }
    private val service = JetBrainsAiUsageService.getInstance()
    private val listener: (JetBrainsAiUsage) -> Unit = ::render

    init {
        isOpaque = false
        add(header, BorderLayout.NORTH)
        add(JPanel(GridLayout(0, 1, 0, 4)).apply {
            isOpaque = false
            add(subscription)
            add(topUp)
        }, BorderLayout.CENTER)
        add(status, BorderLayout.SOUTH)
        service.addListener(listener)
        render(service.current)
    }

    private fun render(usage: JetBrainsAiUsage) {
        val hasDetails = usage.subscription != null || usage.topUp != null
        subscription.render(
            usage.subscription ?: if (!hasDetails) usage.quota else null,
            if (hasDetails) "Subscription" else "Total credits"
        )
        topUp.render(usage.topUp, "Top-up")
        topUp.isVisible = usage.topUp != null
        val details = buildList {
            when {
                usage.unlimited -> add("Unlimited credits")
                usage.quota != null -> add("${formatAiCredits(usage.quota.remaining)} / ${formatAiCredits(usage.quota.total)} credits remaining")
                usage.error != null -> add("Credit balance unavailable")
                else -> add("Loading JetBrains AI credits…")
            }
            usage.error?.let { add(it) }
            usage.resetsAt?.let { add("Subscription resets ${formatResetTime(it)}") }
        }
        header.render(usage.plan, details)
        status.text = usage.error?.let(::compactUsageMessage).orEmpty()
        status.isVisible = usage.error != null
        status.toolTipText = header.toolTipText
        revalidate()
        repaint()
    }

    override fun dispose() = service.removeListener(listener)
}

private class AiCreditCard(title: String) : UsageCard(title) {
    fun render(quota: JetBrainsAiQuota?, title: String) {
        heading.text = title
        remaining.text = quota?.let { "${formatAiCredits(it.remaining)} / ${formatAiCredits(it.total)}" } ?: "—"
        detail.text = quota?.let { "of ${formatAiCredits(it.total)} credits" } ?: "Credits unavailable"
        updateProgress(quota?.percentLeft, usageBarColor(quota?.percentLeft))
    }
}
