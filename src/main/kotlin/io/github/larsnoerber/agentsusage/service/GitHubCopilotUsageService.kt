package io.github.larsnoerber.agentsusage.service

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import io.github.larsnoerber.agentsusage.model.GitHubCopilotUsage

@Service(Service.Level.APP)
class GitHubCopilotUsageService : Disposable {
    private val reader = GitHubCopilotUsageReader()
    private val log = Logger.getInstance(GitHubCopilotUsageService::class.java)
    private val polling = UsagePolling(GitHubCopilotUsage(), reader::read) { error ->
        log.debug("Unable to read GitHub Copilot usage", error)
        GitHubCopilotUsage(error = "GitHub Copilot usage is unavailable with this Copilot version")
    }

    val current: GitHubCopilotUsage
        get() = polling.current

    fun addListener(listener: (GitHubCopilotUsage) -> Unit) = polling.addListener(listener)
    fun removeListener(listener: (GitHubCopilotUsage) -> Unit) = polling.removeListener(listener)
    fun refresh() = polling.refresh()
    override fun dispose() = polling.dispose()

    companion object {
        fun getInstance(): GitHubCopilotUsageService =
            ApplicationManager.getApplication().getService(GitHubCopilotUsageService::class.java)
        fun isAvailable(): Boolean = GitHubCopilotUsageReader.isAvailable()
    }
}
