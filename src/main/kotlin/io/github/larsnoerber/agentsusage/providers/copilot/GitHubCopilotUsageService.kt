package io.github.larsnoerber.agentsusage.providers.copilot

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import io.github.larsnoerber.agentsusage.core.UsageSource
import io.github.larsnoerber.agentsusage.core.refresh.UsagePolling
import io.github.larsnoerber.agentsusage.settings.AgentsUsageSettings

@Service(Service.Level.APP)
class GitHubCopilotUsageService : UsageSource<GitHubCopilotUsage>, Disposable {
    private val reader = GitHubCopilotUsageReader()
    private val log = Logger.getInstance(GitHubCopilotUsageService::class.java)
    private val polling = UsagePolling(GitHubCopilotUsage(), reader::read, { AgentsUsageSettings.getInstance().refreshIntervalSeconds }) { error ->
        log.debug("Unable to read GitHub Copilot usage", error)
        GitHubCopilotUsage(error = "GitHub Copilot usage is unavailable with this Copilot version")
    }

    override val current: GitHubCopilotUsage
        get() = polling.current

    override fun addListener(listener: (GitHubCopilotUsage) -> Unit) = polling.addListener(listener)
    override fun removeListener(listener: (GitHubCopilotUsage) -> Unit) = polling.removeListener(listener)
    override fun refresh() = polling.refresh()
    override fun dispose() = polling.dispose()

    companion object {
        fun getInstance(): GitHubCopilotUsageService =
            ApplicationManager.getApplication().getService(GitHubCopilotUsageService::class.java)
        fun isAvailable(): Boolean = GitHubCopilotUsageReader.isAvailable()
    }
}
