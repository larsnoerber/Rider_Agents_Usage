package io.github.larsnoerber.agentsusage.providers.codex.ui

import com.intellij.openapi.Disposable
import com.intellij.ui.components.JBLabel
import io.github.larsnoerber.agentsusage.providers.codex.CodexUsage
import io.github.larsnoerber.agentsusage.providers.codex.CodexUsageService
import io.github.larsnoerber.agentsusage.settings.AgentsUsageSettings
import io.github.larsnoerber.agentsusage.ui.components.ProviderHeader
import io.github.larsnoerber.agentsusage.ui.components.compactUsageMessage
import io.github.larsnoerber.agentsusage.ui.components.usageTooltip
import io.github.larsnoerber.agentsusage.ui.components.warningColor
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.GridLayout
import java.awt.event.HierarchyEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.Timer

internal class CodexUsagePanel(actions: JComponent) : JPanel(BorderLayout(0, 5)), Disposable {
    private val header = ProviderHeader("OpenAI", actions)
    private val fiveHour = CodexUsageCard("5 hours")
    private val weekly = CodexUsageCard("This week")
    private val stateDot = JBLabel("●")
    private val stateText = JBLabel()
    private val notice = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
    private val service = CodexUsageService.getInstance()
    private var disposed = false
    private val listener: (CodexUsage) -> Unit = ::render
    private val timer = Timer(1_000) {
        fiveHour.updateCountdown()
        weekly.updateCountdown()
    }

    init {
        isOpaque = false
        add(header, BorderLayout.NORTH)
        add(JPanel(GridLayout(0, 1, 0, 4)).apply {
            isOpaque = false
            add(fiveHour)
            add(weekly)
        }, BorderLayout.CENTER)
        add(notice.apply {
            isOpaque = false
            stateDot.font = stateDot.font.deriveFont(10f)
            stateDot.foreground = warningColor()
            stateText.foreground = warningColor()
            add(stateDot)
            add(stateText)
        }, BorderLayout.SOUTH)
        service.addListener(listener)
        render(service.current)
        addHierarchyListener { event ->
            if (event.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L) {
                if (!disposed && isShowing) timer.start() else timer.stop()
            }
        }
    }

    private fun render(usage: CodexUsage) {
        fiveHour.render(usage.fiveHourLeft, usage.fiveHourReset, usage.fiveHourResetsAt)
        weekly.render(usage.weeklyLeft, usage.weeklyReset, usage.weeklyResetsAt)
        val info = usage.error ?: "Refreshes every ${AgentsUsageSettings.getInstance().refreshIntervalSeconds} sec · Credits ${usage.credits ?: "—"}"
        header.render(usage.plan, listOf(info))
        notice.isVisible = usage.error != null
        stateText.text = usage.error?.let(::compactUsageMessage).orEmpty()
        val tooltip = usageTooltip("OpenAI Codex", listOf(info))
        stateText.toolTipText = tooltip
        stateDot.toolTipText = tooltip
        revalidate()
        repaint()
    }

    override fun dispose() {
        disposed = true
        timer.stop()
        service.removeListener(listener)
    }
}
