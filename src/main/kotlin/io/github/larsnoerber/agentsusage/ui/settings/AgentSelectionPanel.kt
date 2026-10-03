package io.github.larsnoerber.agentsusage.ui.settings

import com.intellij.ui.components.JBCheckBox
import io.github.larsnoerber.agentsusage.application.UsageRefreshCoordinator
import io.github.larsnoerber.agentsusage.settings.AgentsUsageSettings
import java.awt.GridLayout
import javax.swing.JPanel

/** Shared checkbox editor for IDE Settings and the Tool Window configuration. */
internal class AgentSelectionPanel : JPanel(GridLayout(0, 1, 0, 2)) {
    private val openAi = JBCheckBox("OpenAI")
    private val jetBrainsAi = JBCheckBox("JetBrains AI")
    private val copilot = JBCheckBox("GitHub Copilot")

    init {
        isOpaque = false
        listOf(openAi, jetBrainsAi, copilot).forEach {
            it.isOpaque = false
            add(it)
        }
        toolTipText = "Selected agents appear in the overview and status bar. Optional providers require their plugins."
        reset()
    }

    fun reset() {
        val state = AgentsUsageSettings.getInstance().state
        openAi.isSelected = state.showOpenAi
        jetBrainsAi.isSelected = state.showJetBrainsAi
        copilot.isSelected = state.showCopilot
    }

    fun isModified(): Boolean {
        val state = AgentsUsageSettings.getInstance().state
        return openAi.isSelected != state.showOpenAi || jetBrainsAi.isSelected != state.showJetBrainsAi ||
            copilot.isSelected != state.showCopilot
    }

    fun applySelection() = UsageRefreshCoordinator.changeVisibleAgents(
        openAi.isSelected, jetBrainsAi.isSelected, copilot.isSelected
    )
}
