package io.github.larsnoerber.agentsusage.ui.toolwindow

import com.intellij.openapi.Disposable
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import io.github.larsnoerber.agentsusage.application.UsageRefreshCoordinator
import io.github.larsnoerber.agentsusage.providers.codex.ui.CodexUsagePanel
import io.github.larsnoerber.agentsusage.providers.copilot.GitHubCopilotUsageService
import io.github.larsnoerber.agentsusage.providers.copilot.ui.GitHubCopilotUsagePanel
import io.github.larsnoerber.agentsusage.providers.jetbrainsai.JetBrainsAiUsageService
import io.github.larsnoerber.agentsusage.providers.jetbrainsai.ui.JetBrainsAiUsagePanel
import io.github.larsnoerber.agentsusage.ui.components.UsageSurface
import io.github.larsnoerber.agentsusage.ui.components.UsageToolbar
import io.github.larsnoerber.agentsusage.ui.settings.RefreshSettingsPanel
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Container
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.JPanel

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
    private val codex = CodexUsagePanel(UsageToolbar(UsageRefreshCoordinator::refreshAll, ::showSettings))
    private val jetBrainsAi = if (JetBrainsAiUsageService.isAvailable()) JetBrainsAiUsagePanel() else null
    private val copilot = if (GitHubCopilotUsageService.isAvailable()) GitHubCopilotUsagePanel() else null
    private val settings = RefreshSettingsPanel { showPage(OVERVIEW) }

    init {
        isOpaque = false
        border = JBUI.Borders.empty(6)
        pages.add(UsageSurface().apply {
            addRow(codex)
            jetBrainsAi?.let { addRow(it, top = 10) }
            copilot?.let { addRow(it, top = 10) }
        }, OVERVIEW)
        pages.add(settings, SETTINGS)
        add(JBScrollPane(JPanel(BorderLayout()).apply {
            isOpaque = false
            add(pages, BorderLayout.NORTH)
        }).apply { border = BorderFactory.createEmptyBorder() }, BorderLayout.CENTER)
    }

    private fun showSettings() {
        settings.resetFromSettings()
        showPage(SETTINGS)
    }

    private fun showPage(name: String) {
        pageLayout.show(pages, name)
        pages.revalidate()
        pages.repaint()
    }

    override fun dispose() {
        codex.dispose()
        jetBrainsAi?.dispose()
        copilot?.dispose()
    }

    private companion object {
        const val OVERVIEW = "overview"
        const val SETTINGS = "settings"
    }
}
