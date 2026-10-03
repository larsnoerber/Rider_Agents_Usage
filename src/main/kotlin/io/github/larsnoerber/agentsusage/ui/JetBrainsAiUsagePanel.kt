package io.github.larsnoerber.agentsusage.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import io.github.larsnoerber.agentsusage.model.JetBrainsAiQuota
import io.github.larsnoerber.agentsusage.model.JetBrainsAiUsage
import io.github.larsnoerber.agentsusage.model.formatAiCredits
import io.github.larsnoerber.agentsusage.model.formatSubscriptionPlan
import io.github.larsnoerber.agentsusage.service.JetBrainsAiUsageService
import java.awt.BorderLayout
import java.awt.Font
import java.awt.GridLayout
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.swing.JPanel

internal class JetBrainsAiUsagePanel : JPanel(BorderLayout(0, 8)), Disposable {
    private val balance = JBLabel("Loading JetBrains AI credits…")
    private val subscriptionPlan = JBLabel("Subscription: Unknown")
    private val subscription = AiCreditCard("Subscription")
    private val topUp = AiCreditCard("Top-up")
    private val status = JBLabel(" ")
    private val service = JetBrainsAiUsageService.getInstance()
    private val listener: (JetBrainsAiUsage) -> Unit = ::render

    init {
        isOpaque = false
        add(JPanel(BorderLayout(0, 6)).apply {
            isOpaque = false
            add(JBLabel("JetBrains AI").apply { font = font.deriveFont(Font.BOLD, 15f) }, BorderLayout.NORTH)
            add(subscriptionPlan.apply { foreground = JBColor.GRAY }, BorderLayout.CENTER)
            add(balance, BorderLayout.SOUTH)
        }, BorderLayout.NORTH)
        add(JPanel(GridLayout(0, 1, 0, 10)).apply {
            isOpaque = false
            add(subscription)
            add(topUp)
        }, BorderLayout.CENTER)
        add(status, BorderLayout.SOUTH)
        service.addListener(listener)
        render(service.current)
    }

    private fun render(usage: JetBrainsAiUsage) {
        subscriptionPlan.text = "Subscription: ${formatSubscriptionPlan(usage.plan)}"
        balance.text = when {
            usage.unlimited -> "Unlimited credits"
            usage.quota != null -> "${formatAiCredits(usage.quota.remaining)} / ${formatAiCredits(usage.quota.total)} credits remaining"
            usage.error != null -> "Credit balance unavailable"
            else -> "Loading JetBrains AI credits…"
        }
        balance.foreground = aiCreditColor(usage)
        val hasDetails = usage.subscription != null || usage.topUp != null
        subscription.render(usage.subscription ?: if (!hasDetails) usage.quota else null,
            if (hasDetails) "Subscription" else "Total credits")
        topUp.render(usage.topUp, "Top-up")
        status.foreground = JBColor.GRAY
        status.text = when {
            usage.error != null -> "<html>${StringUtil.escapeXmlEntities(usage.error)}</html>"
            usage.resetsAt != null -> "Subscription resets ${formatAiReset(usage.resetsAt)}"
            else -> "Balance provided by JetBrains AI Assistant"
        }
        revalidate()
        repaint()
    }

    override fun dispose() = service.removeListener(listener)
}

private class AiCreditCard(title: String) : UsageCard(title) {
    fun render(quota: JetBrainsAiQuota?, title: String) {
        heading.text = title
        remaining.text = quota?.let { formatAiCredits(it.remaining) } ?: "—"
        detail.text = quota?.let { "of ${formatAiCredits(it.total)} credits" } ?: "Credits unavailable"
        updateProgress(quota?.percentLeft, usageBarColor(quota?.percentLeft))
    }
}

private val resetFormatter = DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.ENGLISH)
    .withZone(ZoneId.systemDefault())

internal fun formatAiReset(epochSeconds: Long): String = resetFormatter.format(Instant.ofEpochSecond(epochSeconds))
