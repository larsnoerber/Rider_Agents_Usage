package io.github.larsnoerber.agentsusage.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import io.github.larsnoerber.agentsusage.model.CopilotQuota
import io.github.larsnoerber.agentsusage.model.GitHubCopilotUsage
import io.github.larsnoerber.agentsusage.service.GitHubCopilotUsageService
import java.awt.Color
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

class GitHubCopilotStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = "GitHubCopilotUsageStatusBar"
    override fun getDisplayName(): String = "GitHub Copilot Usage"
    override fun isAvailable(project: Project): Boolean = GitHubCopilotUsageService.isAvailable()
    override fun createWidget(project: Project): StatusBarWidget = GitHubCopilotStatusBarWidget(project)
    override fun disposeWidget(widget: StatusBarWidget) = widget.dispose()
}

private class GitHubCopilotStatusBarWidget(private val project: Project) : JPanel(FlowLayout(FlowLayout.LEFT, 3, 0)),
    CustomStatusBarWidget {
    private val indicator = JBLabel("●")
    private val label = JBLabel("GitHub Copilot  loading…")
    private val service = GitHubCopilotUsageService.getInstance()
    private val listener: (GitHubCopilotUsage) -> Unit = ::update

    init {
        isOpaque = false
        indicator.font = indicator.font.deriveFont(10f)
        add(indicator)
        add(label)
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        val mouseHandler = object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (SwingUtilities.isLeftMouseButton(event)) {
                    ToolWindowManager.getInstance(project).getToolWindow("Agents Usage")?.show()
                }
            }
        }
        listOf(this, indicator, label).forEach { it.addMouseListener(mouseHandler) }
        service.addListener(listener)
        update(service.current)
    }

    override fun ID(): String = "GitHubCopilotUsageStatusBar"
    override fun getComponent(): JComponent = this
    override fun install(statusBar: StatusBar) = Unit
    override fun dispose() = service.removeListener(listener)

    private fun update(usage: GitHubCopilotUsage) {
        val quota = usage.primary
        label.text = when {
            quota?.unlimited == true -> "GitHub Copilot  unlimited"
            quota?.percentLeft != null -> "GitHub Copilot  ${quota.percentLeft}%"
            usage.error != null -> "GitHub Copilot  —"
            else -> "GitHub Copilot  loading…"
        }
        indicator.foreground = copilotQuotaColor(quota)
        label.foreground = if (usage.error != null) JBColor.GRAY else null
        val lines = mutableListOf("<b>GitHub Copilot usage</b>")
        usage.plan?.let { lines += "Plan: ${StringUtil.escapeXmlEntities(it)}" }
        listOfNotNull(usage.primary, usage.chat, usage.completions).distinct().forEach {
            lines += "${it.title}: ${it.displayRemaining} remaining"
        }
        usage.resetsAt?.let { lines += "Resets: ${formatAiReset(it)}" }
        usage.reportedAt?.let { lines += "Last reported: ${formatAiReset(it)}" }
        lines += StringUtil.escapeXmlEntities(usage.error ?: "Click to open usage details")
        toolTipText = lines.joinToString("<br>", "<html>", "</html>")
        listOf(indicator, label).forEach { it.toolTipText = toolTipText }
        revalidate()
        repaint()
    }
}

internal fun copilotQuotaColor(quota: CopilotQuota?): Color =
    usageBarColor(if (quota?.unlimited == true) 100 else quota?.percentLeft)
