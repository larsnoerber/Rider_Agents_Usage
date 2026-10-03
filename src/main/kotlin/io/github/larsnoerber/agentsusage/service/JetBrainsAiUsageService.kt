package io.github.larsnoerber.agentsusage.service

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import io.github.larsnoerber.agentsusage.model.JetBrainsAiUsage

@Service(Service.Level.APP)
class JetBrainsAiUsageService : Disposable {
    private val reader = JetBrainsAiUsageReader()
    private val log = Logger.getInstance(JetBrainsAiUsageService::class.java)
    private val polling = UsagePolling(JetBrainsAiUsage(), reader::read) { error ->
        log.debug("Unable to read JetBrains AI credits", error)
        JetBrainsAiUsage(error = "JetBrains AI credits are unavailable with this AI Assistant version")
    }

    val current: JetBrainsAiUsage
        get() = polling.current

    fun addListener(listener: (JetBrainsAiUsage) -> Unit) = polling.addListener(listener)
    fun removeListener(listener: (JetBrainsAiUsage) -> Unit) = polling.removeListener(listener)
    fun refresh() = polling.refresh()
    override fun dispose() = polling.dispose()

    companion object {
        fun getInstance(): JetBrainsAiUsageService =
            ApplicationManager.getApplication().getService(JetBrainsAiUsageService::class.java)
        fun isAvailable(): Boolean = JetBrainsAiUsageReader.isAvailable()
    }
}
