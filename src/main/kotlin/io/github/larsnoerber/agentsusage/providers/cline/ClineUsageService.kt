package io.github.larsnoerber.agentsusage.providers.cline

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import io.github.larsnoerber.agentsusage.core.UsageSource
import io.github.larsnoerber.agentsusage.core.agents.AcpAgentInstallation
import io.github.larsnoerber.agentsusage.core.refresh.UsagePolling
import io.github.larsnoerber.agentsusage.core.reflection.loadedPlugin
import io.github.larsnoerber.agentsusage.settings.AgentsUsageSettings

@Service(Service.Level.APP)
class ClineUsageService : UsageSource<ClineUsage>, Disposable {
    private val reader = ClineUsageReader()
    private val accountReader = ClineAccountUsageReader()
    private var snapshot = ClineUsage(error = if (isAvailable()) null else "No supported local Cline usage history found.")
    private val polling = UsagePolling(snapshot, { refresh ->
        if (refresh) {
            val local = reader.read()
            val account = accountReader.read()
            snapshot = local.copy(accountBalanceUsd = account.accountBalanceUsd, accountError = account.accountError)
        }
        snapshot
    }, { AgentsUsageSettings.getInstance().refreshIntervalSeconds.coerceAtLeast(60) },
        { AgentsUsageSettings.getInstance().state.showCline && isAvailable() }) { ClineUsage(error = "Local Cline usage unavailable.") }

    override val current: ClineUsage get() = polling.current
    override fun addListener(listener: (ClineUsage) -> Unit) = polling.addListener(listener)
    override fun removeListener(listener: (ClineUsage) -> Unit) = polling.removeListener(listener)
    override fun refresh() = polling.refresh()
    override fun dispose() { polling.dispose(); reader.close(); accountReader.close() }

    companion object {
        const val PLUGIN_ID = "bot.cline"
        fun isInstalled(): Boolean = AcpAgentInstallation.isInstalled("cline") || loadedPlugin(PLUGIN_ID) != null
        fun isAvailable(): Boolean = isInstalled() &&
            (ClineUsageReader.hasLocalUsageSource() || ClineAccountUsageReader.hasLocalAccount())

        fun getInstance(): ClineUsageService = ApplicationManager.getApplication().getService(ClineUsageService::class.java)
    }
}
