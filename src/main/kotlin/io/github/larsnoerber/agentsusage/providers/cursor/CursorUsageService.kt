package io.github.larsnoerber.agentsusage.providers.cursor

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import io.github.larsnoerber.agentsusage.core.UsageSource
import io.github.larsnoerber.agentsusage.core.agents.AcpAgentInstallation
import io.github.larsnoerber.agentsusage.core.refresh.UsagePolling
import io.github.larsnoerber.agentsusage.settings.AgentsUsageSettings

@Service(Service.Level.APP)
class CursorUsageService : UsageSource<CursorUsage>, Disposable {
    private val reader = CursorUsageReader()
    private var snapshot = CursorUsage(error = if (isAvailable()) null else
        "Sign in to the Cursor ACP agent to load usage.")
    private val polling = UsagePolling(snapshot, { refresh ->
        if (refresh) snapshot = reader.read()
        snapshot
    }, { AgentsUsageSettings.getInstance().refreshIntervalSeconds.coerceAtLeast(60) },
        { AgentsUsageSettings.getInstance().state.showCursor && isAvailable() }) { CursorUsage(error = "Cursor usage unavailable.") }

    override val current: CursorUsage get() = polling.current
    override fun addListener(listener: (CursorUsage) -> Unit) = polling.addListener(listener)
    override fun removeListener(listener: (CursorUsage) -> Unit) = polling.removeListener(listener)
    override fun refresh() = polling.refresh()
    override fun dispose() { polling.dispose(); reader.close() }

    companion object {
        fun isInstalled(): Boolean = AcpAgentInstallation.isInstalled("cursor")
        fun isAvailable(): Boolean = isInstalled() && CursorUsageReader.hasLocalAccount()

        fun getInstance(): CursorUsageService = ApplicationManager.getApplication().getService(CursorUsageService::class.java)
    }
}
