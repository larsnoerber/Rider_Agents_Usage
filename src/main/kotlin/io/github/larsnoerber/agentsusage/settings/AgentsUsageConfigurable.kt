package io.github.larsnoerber.agentsusage.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import io.github.larsnoerber.agentsusage.application.UsageRefreshCoordinator
import io.github.larsnoerber.agentsusage.providers.codex.CodexUsageService
import io.github.larsnoerber.agentsusage.ui.settings.AgentSelectionPanel
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class AgentsUsageConfigurable : Configurable {
    private val pathField = JBTextField()
    private val intervalField = JBTextField()
    private val panel = JPanel(BorderLayout())
    private val detectButton = JButton("Auto-detect")
    private val agents = AgentSelectionPanel()

    init {
        val pathRow = JPanel(BorderLayout(6, 0))
        pathRow.add(pathField, BorderLayout.CENTER)
        pathRow.add(detectButton, BorderLayout.EAST)
        val form = FormBuilder.createFormBuilder()
            .addComponent(JBLabel("Agent settings"))
            .addComponent(JBLabel("Visible agents (overview and status bar):"))
            .addComponent(agents)
            .addComponent(JBLabel("<html>Refresh settings apply to OpenAI, JetBrains AI, and GitHub Copilot.</html>"))
            .addLabeledComponent(JBLabel("Auto-refresh interval for all agents (seconds):"), intervalField)
            .addLabeledComponent(JBLabel("OpenAI (Codex) CLI path (leave empty for auto-detect):"), pathRow)
            .addComponent(JBLabel("If empty, the system PATH will be used to find codex automatically."))
            .panel
        panel.add(form, BorderLayout.NORTH)
        detectButton.addActionListener { detectCodexPath() }
    }

    override fun getDisplayName(): String = "Agents Usage"
    override fun getPreferredFocusedComponent(): JComponent = intervalField
    override fun createComponent(): JComponent = panel

    override fun isModified(): Boolean {
        val settings = AgentsUsageSettings.getInstance().state
        return pathField.text != settings.codexPath || intervalField.text.toIntOrNull() != settings.refreshSeconds || agents.isModified()
    }

    override fun reset() {
        val settings = AgentsUsageSettings.getInstance().state
        pathField.text = settings.codexPath
        intervalField.text = settings.refreshSeconds.toString()
        agents.reset()
    }

    override fun apply() {
        val interval = intervalField.text.toIntOrNull()
        if (interval == null || interval !in AgentsUsageSettings.MIN_REFRESH_SECONDS..AgentsUsageSettings.MAX_REFRESH_SECONDS) {
            Messages.showErrorDialog("Refresh interval must be an integer between 10 and 3600 seconds.", "Agents Usage")
            return
        }
        val settings = AgentsUsageSettings.getInstance().state
        settings.codexPath = pathField.text.trim()
        agents.applySelection()
        UsageRefreshCoordinator.changeRefreshInterval(interval)
    }

    private fun detectCodexPath() {
        detectButton.isEnabled = false
        ApplicationManager.getApplication().executeOnPooledThread {
            val path = CodexUsageService.getInstance().detectCodexPath()
            ApplicationManager.getApplication().invokeLater {
                detectButton.isEnabled = true
                if (path == null) {
                    Messages.showWarningDialog(
                        "Codex CLI was not found on the system PATH. Please install and sign in to Codex CLI first, or enter the full path to codex.cmd manually.",
                        "Agents Usage"
                    )
                } else {
                    pathField.text = path
                }
            }
        }
    }
}
