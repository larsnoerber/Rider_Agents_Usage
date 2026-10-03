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
import io.github.larsnoerber.agentsusage.model.JetBrainsAiUsage
import io.github.larsnoerber.agentsusage.model.formatAiCredits
import io.github.larsnoerber.agentsusage.service.JetBrainsAiUsageService
import java.awt.Color
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

class JetBrainsAiStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = "JetBrainsAiCreditsStatusBar"
    override fun getDisplayName(): String = "JetBrains AI Credits"
    override fun isAvailable(project: Project): Boolean = JetBrainsAiUsageService.isAvailable()
    override fun createWidget(project: Project): StatusBarWidget = JetBrainsAiStatusBarWidget(project)
    override fun disposeWidget(widget: StatusBarWidget) = widget.dispose()
}

private class JetBrainsAiStatusBarWidget(private val project: Project) : JPanel(FlowLayout(FlowLayout.LEFT, 3, 0)),
    CustomStatusBarWidget {
    private val indicator = JBLabel("●")
    private val label = JBLabel("JetBrains AI  loading…")
    private val service = JetBrainsAiUsageService.getInstance()
    private val listener: (JetBrainsAiUsage) -> Unit = ::update

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

    override fun ID(): String = "JetBrainsAiCreditsStatusBar"
    override fun getComponent(): JComponent = this
    override fun install(statusBar: StatusBar) = Unit
    override fun dispose() = service.removeListener(listener)

    private fun update(usage: JetBrainsAiUsage) {
        label.text = when {
            usage.unlimited -> "JetBrains AI  unlimited"
            usage.quota != null -> "JetBrains AI  ${formatAiCredits(usage.quota.remaining)} cr"
            usage.error != null -> "JetBrains AI  —"
            else -> "JetBrains AI  loading…"
        }
        indicator.foreground = aiCreditColor(usage)
        label.foreground = if (usage.error != null) JBColor.GRAY else null
        val lines = mutableListOf("<b>JetBrains AI credits</b>")
        usage.quota?.let { lines += "Remaining: ${formatAiCredits(it.remaining)} / ${formatAiCredits(it.total)} credits" }
        usage.subscription?.let { lines += "Subscription: ${formatAiCredits(it.remaining)} / ${formatAiCredits(it.total)} credits" }
        usage.topUp?.let { lines += "Top-up: ${formatAiCredits(it.remaining)} / ${formatAiCredits(it.total)} credits" }
        usage.resetsAt?.let { lines += "Subscription resets: ${formatAiReset(it)}" }
        if (usage.unlimited) lines += "Unlimited quota"
        lines += StringUtil.escapeXmlEntities(usage.error ?: "Click to open usage details")
        toolTipText = lines.joinToString("<br>", "<html>", "</html>")
        listOf(indicator, label).forEach { it.toolTipText = toolTipText }
        revalidate()
        repaint()
    }
}

internal fun aiCreditColor(usage: JetBrainsAiUsage): Color = usageBarColor(
    if (usage.error != null) null else if (usage.unlimited) 100 else usage.quota?.percentLeft
)
