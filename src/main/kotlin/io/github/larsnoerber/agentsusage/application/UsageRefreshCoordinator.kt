package io.github.larsnoerber.agentsusage.application

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import io.github.larsnoerber.agentsusage.providers.codex.CodexUsageService
import io.github.larsnoerber.agentsusage.providers.codex.ui.CodexUsageStatusBarWidgetFactory
import io.github.larsnoerber.agentsusage.providers.copilot.GitHubCopilotUsageService
import io.github.larsnoerber.agentsusage.providers.copilot.ui.GitHubCopilotStatusBarWidgetFactory
import io.github.larsnoerber.agentsusage.providers.jetbrainsai.JetBrainsAiUsageService
import io.github.larsnoerber.agentsusage.providers.jetbrainsai.ui.JetBrainsAiStatusBarWidgetFactory
import io.github.larsnoerber.agentsusage.settings.AgentsUsageSettings
import java.util.concurrent.CopyOnWriteArrayList

/** Coordinates user actions across providers; persistence itself does not start services. */
internal object UsageRefreshCoordinator {
    private val visibilityListeners = CopyOnWriteArrayList<() -> Unit>()

    fun addVisibilityListener(listener: () -> Unit) { visibilityListeners.addIfAbsent(listener) }
    fun removeVisibilityListener(listener: () -> Unit) { visibilityListeners -= listener }

    fun changeVisibleAgents(openAi: Boolean, jetBrainsAi: Boolean, copilot: Boolean) {
        val settings = AgentsUsageSettings.getInstance().state
        val openAiWasEnabled = settings.showOpenAi
        val changed = settings.showOpenAi != openAi || settings.showJetBrainsAi != jetBrainsAi || settings.showCopilot != copilot
        settings.showOpenAi = openAi
        settings.showJetBrainsAi = jetBrainsAi
        settings.showCopilot = copilot
        if (!changed) return
        if (openAi && !openAiWasEnabled) CodexUsageService.getInstance().restartTimer()
        ApplicationManager.getApplication().invokeLater {
            visibilityListeners.forEach { it() }
            ProjectManager.getInstance().openProjects.filterNot { it.isDisposed }.forEach { project ->
                project.getService(StatusBarWidgetsManager::class.java).apply {
                    updateWidget(CodexUsageStatusBarWidgetFactory::class.java)
                    updateWidget(JetBrainsAiStatusBarWidgetFactory::class.java)
                    updateWidget(GitHubCopilotStatusBarWidgetFactory::class.java)
                }
            }
        }
    }

    fun refreshAll() {
        if (AgentsUsageSettings.getInstance().state.showOpenAi) CodexUsageService.getInstance().refresh()
        refreshOptionalProviders()
    }

    fun changeRefreshInterval(seconds: Int) {
        AgentsUsageSettings.getInstance().applyRefreshInterval(seconds)
        // Restarting the CLI timer also schedules its immediate refresh.
        if (AgentsUsageSettings.getInstance().state.showOpenAi) CodexUsageService.getInstance().restartTimer()
        refreshOptionalProviders()
    }

    private fun refreshOptionalProviders() {
        val settings = AgentsUsageSettings.getInstance().state
        if (settings.showJetBrainsAi && JetBrainsAiUsageService.isAvailable()) JetBrainsAiUsageService.getInstance().refresh()
        if (settings.showCopilot && GitHubCopilotUsageService.isAvailable()) GitHubCopilotUsageService.getInstance().refresh()
    }
}
