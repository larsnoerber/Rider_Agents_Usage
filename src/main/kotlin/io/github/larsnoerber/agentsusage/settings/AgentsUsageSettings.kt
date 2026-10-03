package io.github.larsnoerber.agentsusage.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import io.github.larsnoerber.agentsusage.service.CodexUsageService
import io.github.larsnoerber.agentsusage.service.GitHubCopilotUsageService
import io.github.larsnoerber.agentsusage.service.JetBrainsAiUsageService

data class AgentsUsageState(
    @Volatile var codexPath: String = "",
    @Volatile var refreshSeconds: Int = 60
)

// Retain the storage identifiers so existing CLI paths and refresh intervals survive the rename.
@State(name = "CodexUsageSettings", storages = [Storage("codex-usage.xml")])
class AgentsUsageSettings : PersistentStateComponent<AgentsUsageState> {
    @Volatile private var state = AgentsUsageState()

    val refreshIntervalSeconds: Int
        get() = state.refreshSeconds.coerceIn(10, 3600)

    override fun getState(): AgentsUsageState = state
    override fun loadState(state: AgentsUsageState) {
        this.state = state.copy(refreshSeconds = state.refreshSeconds.coerceIn(10, 3600))
    }

    fun applyRefreshInterval(seconds: Int) {
        state.refreshSeconds = seconds.coerceIn(10, 3600)
        CodexUsageService.getInstance().restartTimer()
        if (JetBrainsAiUsageService.isAvailable()) JetBrainsAiUsageService.getInstance().refresh()
        if (GitHubCopilotUsageService.isAvailable()) GitHubCopilotUsageService.getInstance().refresh()
    }

    companion object {
        fun getInstance(): AgentsUsageSettings =
            ApplicationManager.getApplication().getService(AgentsUsageSettings::class.java)
    }
}
