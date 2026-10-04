package io.github.larsnoerber.agentsusage.providers.cline.ui

import com.intellij.openapi.Disposable
import com.intellij.ui.components.JBLabel
import io.github.larsnoerber.agentsusage.providers.cline.ClineUsage
import io.github.larsnoerber.agentsusage.providers.cline.ClineUsageService
import io.github.larsnoerber.agentsusage.ui.components.*
import java.awt.BorderLayout
import javax.swing.JPanel

internal class ClineUsagePanel : JPanel(BorderLayout(0, 4)), Disposable {
    private val header = ProviderHeader("Cline")
    private val details = ProviderUsageDetails("cline", percent = false)
    private val summary = UsageSummary(details, header)
    private val status = JBLabel().apply { foreground = secondaryTextColor() }
    private val service = ClineUsageService.getInstance()
    private val listener: (ClineUsage) -> Unit = ::render

    init {
        isOpaque = false
        add(header, BorderLayout.NORTH); add(summary, BorderLayout.CENTER); add(status, BorderLayout.SOUTH)
        service.addListener(listener)
        render(service.current)
    }

    private fun render(usage: ClineUsage) {
        header.render(if (usage.accountBalanceUsd != null) "Cline credits" else null,
            listOf("Local task and SDK usage across recorded history", "Account credits from the existing Cline CLI/ACP sign-in"))
        val available = usage.sources > 0
        summary.render("Cline details", listOf(
            "Tokens" to if (available) usage.totalTokens.formatted() else "—",
            "Tasks" to if (available) usage.tasks.toString() else "—",
            "Input" to if (available) usage.inputTokens.formatted() else "—",
            "Cost" to (usage.costUsd?.let { "$%.2f".format(java.util.Locale.US, it) } ?: "—")
        ), listOf("Account balance" to (usage.accountBalanceUsd?.usd() ?: "—"),
            "Account status" to (usage.accountError ?: if (usage.accountBalanceUsd != null) "Connected" else "Loading"),
            "Output" to usage.outputTokens.formatted(), "Cache read" to usage.cacheReadTokens.formatted(),
            "Cache write" to usage.cacheWriteTokens.formatted(), "Sources" to usage.sources.toString(),
            "Period" to "All recorded local tasks", "Cost coverage" to "Sum of reported costs; missing costs are excluded",
            "Quota" to "Pay-as-you-go credits; no percentage limit reported", "Details" to (usage.error ?: "Local usage available")))
        details.render(if (available) listOf(
            UsageDetailMetric("input", "Input", usage.inputTokens.toDouble()),
            UsageDetailMetric("output", "Output", usage.outputTokens.toDouble())
        ) else emptyList(), record = usage.error == null && usage.updatedAt > 0, observedAt = usage.updatedAt)
        val message = usage.accountBalanceUsd?.let { "Account balance: ${it.usd()}" }
            ?: usage.accountError ?: usage.error ?: "Local task usage available."
        status.text = compactUsageMessage(message)
        status.toolTipText = usageTooltip("Cline usage", listOfNotNull(message, usage.error,
            "Local tokens and recorded costs are separate from the account balance."))
        revalidate(); repaint()
    }

    override fun dispose() { service.removeListener(listener); summary.dispose() }
    private fun Long.formatted(): String = "%,d".format(this)
    private fun Double.usd(): String = "$%.2f".format(java.util.Locale.US, this)
}
