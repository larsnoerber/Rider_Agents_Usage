package io.github.larsnoerber.agentsusage.ui.settings

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ui.JBUI
import io.github.larsnoerber.agentsusage.application.UsageRefreshCoordinator
import io.github.larsnoerber.agentsusage.core.agents.AcpAgentInstallation
import io.github.larsnoerber.agentsusage.settings.AgentsUsageSettings
import io.github.larsnoerber.agentsusage.ui.components.secondaryTextColor
import java.awt.BorderLayout
import java.awt.event.HierarchyEvent
import javax.swing.BoxLayout
import javax.swing.JPanel

/** Shared checkbox editor for IDE Settings and the Tool Window configuration. */
internal class AgentSelectionPanel : JPanel() {
    private val openAi = JBCheckBox("OpenAI")
    private val jetBrainsAi = JBCheckBox("JetBrains AI")
    private val copilot = JBCheckBox("GitHub Copilot")
    private val claudeCode = JBCheckBox("Claude (subscription quota and local tokens)")
    private val cursor = JBCheckBox("Cursor")
    private val acpNotices = mutableListOf<Pair<String, JBLabel>>()
    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
        add(agentRow(openAi, "codex-acp"))
        add(agentRow(jetBrainsAi))
        add(agentRow(copilot, "github-copilot"))
        add(agentRow(claudeCode, "claude-acp"))
        add(agentRow(cursor, "cursor"))
        toolTipText = "Selected installed agents appear in the overview and status bar."
        reset()
        addHierarchyListener { event ->
            if (event.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L && isShowing) updateInstallationNotices()
        }
    }

    private fun agentRow(
        checkBox: JBCheckBox,
        agentId: String? = null
    ): JPanel =
        JPanel(BorderLayout(6, 0)).apply {
            isOpaque = false
            border = JBUI.Borders.emptyBottom(2)
            checkBox.isOpaque = false
            add(checkBox, BorderLayout.NORTH)
            if (agentId != null) {
                val name = checkBox.text.substringBefore(" (")
                val message = "$name ACP package is not installed. Install it from Rider's ACP Registry."
                add(JBLabel("<html>${StringUtil.escapeXmlEntities(message)}</html>").apply {
                    foreground = secondaryTextColor()
                    border = JBUI.Borders.empty(0, 24, 4, 0)
                    toolTipText = message
                    acpNotices += agentId to this
                }, BorderLayout.CENTER)
            }
        }

    private fun updateInstallationNotices() {
        acpNotices.forEach { (agentId, notice) -> notice.isVisible = !AcpAgentInstallation.isInstalled(agentId) }
        revalidate()
        repaint()
    }

    fun reset() {
        updateInstallationNotices()
        val state = AgentsUsageSettings.getInstance().state
        openAi.isSelected = state.showOpenAi
        jetBrainsAi.isSelected = state.showJetBrainsAi
        copilot.isSelected = state.showCopilot
        claudeCode.isSelected = state.showClaudeCode
        cursor.isSelected = state.showCursor
    }

    fun isModified(): Boolean {
        val state = AgentsUsageSettings.getInstance().state
        return openAi.isSelected != state.showOpenAi || jetBrainsAi.isSelected != state.showJetBrainsAi ||
            copilot.isSelected != state.showCopilot || claudeCode.isSelected != state.showClaudeCode ||
            cursor.isSelected != state.showCursor
    }

    fun applySelection() = UsageRefreshCoordinator.changeVisibleAgents(
        openAi.isSelected, jetBrainsAi.isSelected, copilot.isSelected, claudeCode.isSelected, cursor.isSelected
    )

}
