package io.github.larsnoerber.agentsusage.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

data class AgentsUsageState(
    @Volatile var codexPath: String = "",
    @Volatile var refreshSeconds: Int = AgentsUsageSettings.DEFAULT_REFRESH_SECONDS,
    @Volatile var showOpenAi: Boolean = true,
    @Volatile var showJetBrainsAi: Boolean = true,
    @Volatile var showCopilot: Boolean = true,
    @Volatile var weeklyInsightsExpanded: Boolean = true
)

// Keep the original identifiers so saved paths and refresh intervals survive upgrades.
@State(name = "CodexUsageSettings", storages = [Storage("codex-usage.xml")])
class AgentsUsageSettings : PersistentStateComponent<AgentsUsageState> {
    @Volatile private var state = AgentsUsageState()

    val refreshIntervalSeconds: Int
        get() = state.refreshSeconds.coerceIn(MIN_REFRESH_SECONDS, MAX_REFRESH_SECONDS)

    override fun getState(): AgentsUsageState = state

    override fun loadState(state: AgentsUsageState) {
        this.state = state.copy(refreshSeconds = state.refreshSeconds.coerceIn(MIN_REFRESH_SECONDS, MAX_REFRESH_SECONDS))
    }

    /** Only persists the choice; UsageRefreshCoordinator applies it to running providers. */
    fun applyRefreshInterval(seconds: Int) {
        state.refreshSeconds = seconds.coerceIn(MIN_REFRESH_SECONDS, MAX_REFRESH_SECONDS)
    }

    companion object {
        const val MIN_REFRESH_SECONDS = 10
        const val MAX_REFRESH_SECONDS = 3600
        const val DEFAULT_REFRESH_SECONDS = 60

        fun getInstance(): AgentsUsageSettings =
            ApplicationManager.getApplication().getService(AgentsUsageSettings::class.java)
    }
}
