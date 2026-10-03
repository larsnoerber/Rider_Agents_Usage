package io.github.larsnoerber.agentsusage.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import io.github.larsnoerber.agentsusage.model.CopilotQuota
import io.github.larsnoerber.agentsusage.model.GitHubCopilotUsage
import io.github.larsnoerber.agentsusage.model.formatSubscriptionPlan
import io.github.larsnoerber.agentsusage.service.GitHubCopilotUsageService
import java.awt.BorderLayout
import java.awt.Font
import java.awt.GridLayout
import javax.swing.JPanel

internal class GitHubCopilotUsagePanel : JPanel(BorderLayout(0, 8)), Disposable {
    private val subscription = JBLabel("Subscription: Unknown")
    private val cards = JPanel(GridLayout(0, 1, 0, 10)).apply { isOpaque = false }
    private val usageCards = LinkedHashMap<String, CopilotUsageCard>()
    private val status = JBLabel(" ")
    private val service = GitHubCopilotUsageService.getInstance()
    private val listener: (GitHubCopilotUsage) -> Unit = ::render

    init {
        isOpaque = false
        add(JPanel(BorderLayout(0, 6)).apply {
            isOpaque = false
            add(JBLabel("GitHub Copilot").apply { font = font.deriveFont(Font.BOLD, 15f) }, BorderLayout.NORTH)
            add(subscription.apply { foreground = JBColor.GRAY }, BorderLayout.SOUTH)
        }, BorderLayout.NORTH)
        add(cards, BorderLayout.CENTER)
        add(status.apply { foreground = JBColor.GRAY }, BorderLayout.SOUTH)
        service.addListener(listener)
        render(service.current)
    }

    private fun render(usage: GitHubCopilotUsage) {
        subscription.text = "Subscription: ${formatSubscriptionPlan(usage.plan)}"
        val quotas = listOfNotNull(usage.primary, usage.chat, usage.completions).distinctBy { it.title }
        val titles = quotas.map { it.title }.ifEmpty { listOf("Usage") }
        if (usageCards.keys.toList() != titles) {
            cards.removeAll()
            usageCards.clear()
            titles.forEach { title ->
                usageCards[title] = CopilotUsageCard(title).also { cards.add(it) }
            }
        }
        usageCards.forEach { (title, card) -> card.render(quotas.firstOrNull { it.title == title }) }
        status.text = when {
            usage.error != null -> "<html>${StringUtil.escapeXmlEntities(usage.error)}</html>"
            else -> buildList {
                usage.resetsAt?.let { add("Resets ${formatAiReset(it)}") }
                usage.reportedAt?.let { add("Last reported ${formatAiReset(it)}") }
            }.joinToString("<br>", "<html>", "</html>")
        }
        revalidate()
        repaint()
    }

    override fun dispose() = service.removeListener(listener)
}

private class CopilotUsageCard(title: String) : UsageCard(title) {
    fun render(quota: CopilotQuota?) {
        remaining.text = when {
            quota?.unlimited == true -> "Unlimited"
            quota?.remaining != null -> quota.remaining.toString()
            quota?.percentLeft != null -> "${quota.percentLeft}%"
            else -> "—"
        }
        detail.text = when {
            quota?.unlimited == true -> "No usage limit"
            quota?.total != null -> "of ${quota.total} remaining"
            quota?.remaining != null || quota?.percentLeft != null -> "Remaining usage"
            else -> "Usage unavailable"
        }
        val color = copilotQuotaColor(quota)
        updateProgress(if (quota?.unlimited == true) 100 else quota?.percentLeft, color)
    }
}
