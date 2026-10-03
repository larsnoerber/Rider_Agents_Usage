package io.github.larsnoerber.agentsusage.application

import io.github.larsnoerber.agentsusage.providers.codex.CodexUsageService
import io.github.larsnoerber.agentsusage.providers.copilot.GitHubCopilotUsageService
import io.github.larsnoerber.agentsusage.providers.jetbrainsai.JetBrainsAiUsageService
import io.github.larsnoerber.agentsusage.settings.AgentsUsageSettings

/** Coordinates user actions across providers; persistence itself does not start services. */
internal object UsageRefreshCoordinator {
    fun refreshAll() {
        CodexUsageService.getInstance().refresh()
        refreshOptionalProviders()
    }

    fun changeRefreshInterval(seconds: Int) {
        AgentsUsageSettings.getInstance().applyRefreshInterval(seconds)
        // Restarting the CLI timer also schedules its immediate refresh.
        CodexUsageService.getInstance().restartTimer()
        refreshOptionalProviders()
    }

    private fun refreshOptionalProviders() {
        if (JetBrainsAiUsageService.isAvailable()) JetBrainsAiUsageService.getInstance().refresh()
        if (GitHubCopilotUsageService.isAvailable()) GitHubCopilotUsageService.getInstance().refresh()
    }
}
