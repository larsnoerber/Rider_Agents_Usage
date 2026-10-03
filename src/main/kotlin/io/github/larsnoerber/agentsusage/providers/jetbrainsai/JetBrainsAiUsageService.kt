package io.github.larsnoerber.agentsusage.providers.jetbrainsai

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import io.github.larsnoerber.agentsusage.core.UsageSource
import io.github.larsnoerber.agentsusage.core.refresh.UsagePolling
import io.github.larsnoerber.agentsusage.settings.AgentsUsageSettings

@Service(Service.Level.APP)
class JetBrainsAiUsageService : UsageSource<JetBrainsAiUsage>, Disposable {
    private val reader = JetBrainsAiUsageReader()
    private val log = Logger.getInstance(JetBrainsAiUsageService::class.java)
    private val polling = UsagePolling(JetBrainsAiUsage(), reader::read,
        { AgentsUsageSettings.getInstance().refreshIntervalSeconds },
        { AgentsUsageSettings.getInstance().state.showJetBrainsAi }) { error ->
        log.debug("Unable to read JetBrains AI credits", error)
        JetBrainsAiUsage(error = "JetBrains AI credits are unavailable with this AI Assistant version")
    }

    override val current: JetBrainsAiUsage
        get() = polling.current

    override fun addListener(listener: (JetBrainsAiUsage) -> Unit) = polling.addListener(listener)
    override fun removeListener(listener: (JetBrainsAiUsage) -> Unit) = polling.removeListener(listener)
    override fun refresh() = polling.refresh()
    override fun dispose() = polling.dispose()

    companion object {
        fun getInstance(): JetBrainsAiUsageService =
            ApplicationManager.getApplication().getService(JetBrainsAiUsageService::class.java)
        fun isAvailable(): Boolean = JetBrainsAiUsageReader.isAvailable()
    }
}
