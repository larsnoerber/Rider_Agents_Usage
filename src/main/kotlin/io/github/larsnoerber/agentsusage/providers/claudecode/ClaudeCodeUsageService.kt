package io.github.larsnoerber.agentsusage.providers.claudecode

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import io.github.larsnoerber.agentsusage.core.UsageSource
import io.github.larsnoerber.agentsusage.core.agents.AcpAgentInstallation
import io.github.larsnoerber.agentsusage.core.refresh.UsagePolling
import io.github.larsnoerber.agentsusage.core.reflection.loadedPlugin
import io.github.larsnoerber.agentsusage.settings.AgentsUsageSettings

@Service(Service.Level.APP)
class ClaudeCodeUsageService : UsageSource<ClaudeCodeUsage>, Disposable {
    private val reader = ClaudeCodeUsageReader()
    private val quotaReader = ClaudeQuotaReader()
    private var quota = ClaudeCodeUsage()
    private val polling = UsagePolling(ClaudeCodeUsage(quotaError = if (isInstalled()) null else
        "Install the Claude agent or Claude Code plugin to load usage."), { refresh ->
        if (refresh) quota = quotaReader.read()
        reader.read().copy(quotas = quota.quotas, plan = quota.plan, quotaError = quota.quotaError, updatedAt = quota.updatedAt)
    },
        { AgentsUsageSettings.getInstance().refreshIntervalSeconds.coerceAtLeast(180) },
        { AgentsUsageSettings.getInstance().state.showClaudeCode && isInstalled() }) {
        ClaudeCodeUsage(error = "Unable to read local Claude Code usage", quotaError = "Claude quota unavailable")
    }

    override val current: ClaudeCodeUsage get() = polling.current
    override fun addListener(listener: (ClaudeCodeUsage) -> Unit) = polling.addListener(listener)
    override fun removeListener(listener: (ClaudeCodeUsage) -> Unit) = polling.removeListener(listener)
    override fun refresh() = polling.refresh()
    override fun dispose() { polling.dispose(); reader.close(); quotaReader.close() }

    companion object {
        const val PLUGIN_ID = "com.anthropic.code.plugin"
        fun isInstalled(): Boolean = AcpAgentInstallation.isInstalled("claude-acp") || loadedPlugin(PLUGIN_ID) != null

        fun getInstance(): ClaudeCodeUsageService =
            ApplicationManager.getApplication().getService(ClaudeCodeUsageService::class.java)
    }
}
