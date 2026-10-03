package io.github.larsnoerber.agentsusage.ui

import io.github.larsnoerber.agentsusage.model.CodexUsage
import io.github.larsnoerber.agentsusage.service.CodexUsageService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import io.github.larsnoerber.agentsusage.model.formatSubscriptionPlan
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import java.awt.Color
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

class CodexUsageStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = "CodexUsageStatusBar"
    override fun getDisplayName(): String = "codex"
    override fun isAvailable(project: Project): Boolean = true
    override fun createWidget(project: Project): StatusBarWidget = CodexUsageStatusBarWidget(project)
    override fun disposeWidget(widget: StatusBarWidget) = widget.dispose()
}

private class CodexUsageStatusBarWidget(private val project: Project) : JPanel(FlowLayout(FlowLayout.LEFT, 3, 0)),
    CustomStatusBarWidget {

    private val fiveHourIndicator = JBLabel("●")
    private val fiveHourLabel = JBLabel("codex  loading…")
    private val weeklyIndicator = JBLabel("●")
    private val weeklyLabel = JBLabel()
    private val listener: (CodexUsage) -> Unit = ::update

    init {
        isOpaque = false
        fiveHourIndicator.font = fiveHourIndicator.font.deriveFont(10f)
        weeklyIndicator.font = weeklyIndicator.font.deriveFont(10f)
        add(fiveHourIndicator)
        add(fiveHourLabel)
        add(weeklyIndicator)
        add(weeklyLabel)
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        val mouseHandler = object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (SwingUtilities.isLeftMouseButton(event)) {
                    ToolWindowManager.getInstance(project).getToolWindow("Agents Usage")?.show()
                }
            }
        }
        addMouseListener(mouseHandler)
        listOf(fiveHourIndicator, fiveHourLabel, weeklyIndicator, weeklyLabel).forEach {
            it.addMouseListener(mouseHandler)
        }
        CodexUsageService.getInstance().addListener(listener)
        update(CodexUsageService.getInstance().current)
    }

    override fun ID(): String = "CodexUsageStatusBar"
    override fun getComponent(): JComponent = this
    override fun install(statusBar: StatusBar) = Unit
    override fun dispose() = CodexUsageService.getInstance().removeListener(listener)

    private fun update(usage: CodexUsage) {
        val five = usage.fiveHourLeft
        val week = usage.weeklyLeft
        if (five == null || week == null) {
            fiveHourIndicator.foreground = JBColor.GRAY
            fiveHourLabel.text = if (usage.error == null) "codex  loading…" else "codex  —"
            fiveHourLabel.foreground = JBColor.GRAY
            weeklyIndicator.isVisible = false
            weeklyLabel.isVisible = false
        } else {
            fiveHourIndicator.foreground = colorFor(five)
            weeklyIndicator.foreground = colorFor(week)
            fiveHourLabel.text = "codex  5h $five% ·"
            weeklyLabel.text = "Wk $week%${if (usage.error == null) "" else " !"}"
            fiveHourLabel.foreground = null
            weeklyLabel.foreground = null
            weeklyIndicator.isVisible = true
            weeklyLabel.isVisible = true
        }
        toolTipText = tooltipFor(usage)
        listOf(fiveHourIndicator, fiveHourLabel, weeklyIndicator, weeklyLabel).forEach {
            it.toolTipText = toolTipText
        }
    }

    private fun colorFor(left: Int?): Color = usageBarColor(left)

    private fun tooltipFor(value: CodexUsage): String = """<html><b>Codex usage</b><br>
        Subscription: ${StringUtil.escapeXmlEntities(formatSubscriptionPlan(value.plan))}<br>
        5h: remaining ${value.fiveHourLeft ?: "—"}% · ${value.fiveHourReset ?: "reset time unknown"}<br>
        Weekly: remaining ${value.weeklyLeft ?: "—"}% · ${value.weeklyReset ?: "reset time unknown"}<br>
        Credits: ${value.credits ?: "—"}<br>
        ${StringUtil.escapeXmlEntities(value.error ?: "Click to open Agents Usage and view details")}</html>""".trimIndent()
}
