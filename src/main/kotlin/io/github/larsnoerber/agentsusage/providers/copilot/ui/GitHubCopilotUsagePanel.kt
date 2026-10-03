package io.github.larsnoerber.agentsusage.providers.copilot.ui

import com.intellij.openapi.Disposable
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import io.github.larsnoerber.agentsusage.core.format.formatResetTime
import io.github.larsnoerber.agentsusage.providers.copilot.CopilotQuota
import io.github.larsnoerber.agentsusage.providers.copilot.GitHubCopilotUsage
import io.github.larsnoerber.agentsusage.providers.copilot.GitHubCopilotUsageService
import io.github.larsnoerber.agentsusage.ui.components.ProviderHeader
import io.github.larsnoerber.agentsusage.ui.components.UsageCard
import io.github.larsnoerber.agentsusage.ui.components.compactUsageMessage
import java.awt.BorderLayout
import java.awt.GridLayout
import javax.swing.JPanel

internal class GitHubCopilotUsagePanel : JPanel(BorderLayout(0, 5)), Disposable {
    private val header = ProviderHeader("GitHub Copilot")
    private val cards = JPanel(GridLayout(0, 1, 0, 4)).apply { isOpaque = false }
    private val usageCards = LinkedHashMap<String, CopilotUsageCard>()
    private val status = JBLabel().apply { foreground = JBColor.GRAY }
    private val service = GitHubCopilotUsageService.getInstance()
    private val listener: (GitHubCopilotUsage) -> Unit = ::render

    init {
        isOpaque = false
        add(header, BorderLayout.NORTH)
        add(cards, BorderLayout.CENTER)
        add(status, BorderLayout.SOUTH)
        service.addListener(listener)
        render(service.current)
    }

    private fun render(usage: GitHubCopilotUsage) {
        val quotas = listOfNotNull(usage.primary, usage.chat, usage.completions).distinctBy { it.title }
        val titles = quotas.map { it.title }.ifEmpty { listOf("Usage") }
        // Available categories depend on the Copilot plan and billing model.
        if (usageCards.keys.toList() != titles) {
            cards.removeAll()
            usageCards.clear()
            titles.forEach { title ->
                usageCards[title] = CopilotUsageCard(title).also { cards.add(it) }
            }
        }
        usageCards.forEach { (title, card) -> card.render(quotas.firstOrNull { it.title == title }) }
        header.render(usage.plan, buildList {
            usage.error?.let { add(it) }
            usage.resetsAt?.let { add("Resets ${formatResetTime(it)}") }
            usage.reportedAt?.let { add("Last reported ${formatResetTime(it)}") }
        })
        status.text = usage.error?.let(::compactUsageMessage).orEmpty()
        status.isVisible = usage.error != null
        status.toolTipText = header.toolTipText
        revalidate()
        repaint()
    }

    override fun dispose() = service.removeListener(listener)
}

private class CopilotUsageCard(title: String) : UsageCard(title) {
    fun render(quota: CopilotQuota?) {
        remaining.text = when {
            quota?.unlimited == true -> "Unlimited"
            quota?.remaining != null && quota.total != null -> "${quota.remaining} / ${quota.total}"
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
        updateProgress(if (quota?.unlimited == true) 100 else quota?.percentLeft, copilotQuotaColor(quota))
    }
}
