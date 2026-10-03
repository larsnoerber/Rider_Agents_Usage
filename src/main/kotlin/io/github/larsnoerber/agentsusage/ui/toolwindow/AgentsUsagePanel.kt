package io.github.larsnoerber.agentsusage.ui.toolwindow

import com.intellij.openapi.Disposable
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import io.github.larsnoerber.agentsusage.application.UsageRefreshCoordinator
import io.github.larsnoerber.agentsusage.PluginVersion
import io.github.larsnoerber.agentsusage.providers.codex.ui.CodexUsagePanel
import io.github.larsnoerber.agentsusage.providers.copilot.GitHubCopilotUsageService
import io.github.larsnoerber.agentsusage.providers.copilot.ui.GitHubCopilotUsagePanel
import io.github.larsnoerber.agentsusage.providers.jetbrainsai.JetBrainsAiUsageService
import io.github.larsnoerber.agentsusage.providers.jetbrainsai.ui.JetBrainsAiUsagePanel
import io.github.larsnoerber.agentsusage.settings.AgentsUsageSettings
import io.github.larsnoerber.agentsusage.ui.components.AgentSection
import io.github.larsnoerber.agentsusage.ui.components.UsageToolbar
import io.github.larsnoerber.agentsusage.ui.settings.RefreshSettingsPanel
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Container
import java.awt.Dimension
import java.awt.Color
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.Rectangle
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.Scrollable
import javax.swing.ScrollPaneConstants

/** Composes provider views and navigation; each child owns its own data subscription. */
internal class AgentsUsagePanel : JPanel(BorderLayout()), Disposable {
    private val pageLayout = object : CardLayout() {
        override fun preferredLayoutSize(parent: Container): Dimension {
            val size = parent.components.firstOrNull { it.isVisible }?.preferredSize ?: Dimension()
            val insets = parent.insets
            return Dimension(size.width + insets.left + insets.right, size.height + insets.top + insets.bottom)
        }
    }
    private val pages = JPanel(pageLayout).apply { isOpaque = false }
    private val overview = JPanel(BorderLayout(0, 5)).apply { isOpaque = false }
    private val overviewContent = JPanel(BorderLayout(0, JBUI.scale(8))).apply { isOpaque = false }
    private val sections = JPanel(GridBagLayout()).apply { isOpaque = false }
    private val insights = WeeklyUsageInsightsPanel()
    private var codex: CodexUsagePanel? = null
    private var jetBrainsAi: JetBrainsAiUsagePanel? = null
    private var copilot: GitHubCopilotUsagePanel? = null
    private var disposed = false
    private val visibilityListener: () -> Unit = { if (!disposed) updateVisibleAgents() }
    private val settings = RefreshSettingsPanel { showPage(OVERVIEW) }

    init {
        isOpaque = false
        border = JBUI.Borders.empty(6)
        overview.add(JPanel(BorderLayout()).apply {
            isOpaque = false
            add(JBLabel("Agents Usage").apply { font = font.deriveFont(Font.BOLD) }, BorderLayout.WEST)
            add(UsageToolbar(UsageRefreshCoordinator::refreshAll, ::showSettings), BorderLayout.EAST)
        }, BorderLayout.NORTH)
        overviewContent.add(sections, BorderLayout.NORTH)
        overviewContent.add(insights, BorderLayout.SOUTH)
        overview.add(overviewContent, BorderLayout.CENTER)
        overview.add(JBLabel("Version ${PluginVersion.current}").apply {
            foreground = JBUI.CurrentTheme.Label.disabledForeground()
            font = font.deriveFont(font.size2D - 1f)
        }, BorderLayout.SOUTH)
        pages.add(overview, OVERVIEW)
        pages.add(settings, SETTINGS)
        add(JBScrollPane(object : JPanel(BorderLayout()), Scrollable {
            override fun getPreferredScrollableViewportSize(): Dimension = preferredSize
            override fun getScrollableTracksViewportWidth(): Boolean = true
            override fun getScrollableTracksViewportHeight(): Boolean = false
            override fun getScrollableUnitIncrement(rect: Rectangle, orientation: Int, direction: Int): Int = JBUI.scale(16)
            override fun getScrollableBlockIncrement(rect: Rectangle, orientation: Int, direction: Int): Int =
                (rect.height - JBUI.scale(16)).coerceAtLeast(JBUI.scale(16))
        }.apply {
            isOpaque = false
            add(pages, BorderLayout.NORTH)
        }).apply {
            border = BorderFactory.createEmptyBorder()
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        }, BorderLayout.CENTER)
        UsageRefreshCoordinator.addVisibilityListener(visibilityListener)
        updateVisibleAgents()
    }

    private fun updateVisibleAgents() {
        codex?.dispose()
        jetBrainsAi?.dispose()
        copilot?.dispose()
        val choices = AgentsUsageSettings.getInstance().state
        codex = if (choices.showOpenAi) CodexUsagePanel() else null
        jetBrainsAi = if (choices.showJetBrainsAi && JetBrainsAiUsageService.isAvailable()) JetBrainsAiUsagePanel() else null
        copilot = if (choices.showCopilot && GitHubCopilotUsageService.isAvailable()) GitHubCopilotUsagePanel() else null
        sections.removeAll()
        listOfNotNull(
            codex?.let { AgentSection(it, Color(57, 174, 153)) },
            jetBrainsAi?.let { AgentSection(it, Color(157, 119, 220)) },
            copilot?.let { AgentSection(it, Color(82, 151, 230)) }
        ).forEachIndexed { index, section ->
            sections.add(section, GridBagConstraints().apply {
                gridx = 0
                gridy = index
                weightx = 1.0
                fill = GridBagConstraints.HORIZONTAL
                anchor = GridBagConstraints.NORTHWEST
                insets = Insets(if (index == 0) 0 else JBUI.scale(6), 0, 0, 0)
            })
        }
        if (sections.componentCount == 0) {
            sections.add(JBLabel("No agents selected or available. Choose agents in settings.").apply {
                toolTipText = text
            })
        }
        insights.refreshView()
        pages.revalidate()
        pages.repaint()
    }

    private fun showSettings() {
        settings.resetFromSettings()
        showPage(SETTINGS)
    }

    internal fun showUsageOverview() {
        showPage(OVERVIEW)
    }

    private fun showPage(name: String) {
        pageLayout.show(pages, name)
        pages.revalidate()
        pages.repaint()
    }

    override fun dispose() {
        disposed = true
        UsageRefreshCoordinator.removeVisibilityListener(visibilityListener)
        codex?.dispose()
        jetBrainsAi?.dispose()
        copilot?.dispose()
        insights.dispose()
    }

    private companion object {
        const val OVERVIEW = "overview"
        const val SETTINGS = "settings"
    }
}
