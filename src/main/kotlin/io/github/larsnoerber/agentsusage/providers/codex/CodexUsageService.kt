package io.github.larsnoerber.agentsusage.providers.codex

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.concurrency.AppExecutorUtil
import io.github.larsnoerber.agentsusage.core.UsageSource
import io.github.larsnoerber.agentsusage.settings.AgentsUsageSettings
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.APP)
class CodexUsageService : UsageSource<CodexUsage>, Disposable {
    private val log = Logger.getInstance(CodexUsageService::class.java)
    private val reader = CodexUsageReader()
    private val listeners = CopyOnWriteArrayList<(CodexUsage) -> Unit>()
    private val reading = AtomicBoolean()
    @Volatile private var disposed = false
    private var scheduled: ScheduledFuture<*>? = null
    @Volatile override var current = CodexUsage()
        private set

    init { restartTimer() }

    override fun addListener(listener: (CodexUsage) -> Unit) { listeners.addIfAbsent(listener) }
    override fun removeListener(listener: (CodexUsage) -> Unit) { listeners -= listener }

    @Synchronized
    fun restartTimer() {
        if (disposed) return
        scheduled?.cancel(false)
        val seconds = AgentsUsageSettings.getInstance().refreshIntervalSeconds.toLong()
        scheduled = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
            { refresh() }, 0, seconds, TimeUnit.SECONDS
        )
    }

    override fun refresh() {
        if (disposed || !AgentsUsageSettings.getInstance().state.showOpenAi || !reading.compareAndSet(false, true)) return
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                if (!disposed) publish(reader.read(AgentsUsageSettings.getInstance().state.codexPath))
            } catch (e: Exception) {
                if (!disposed) {
                    log.debug("Unable to read Codex usage", e)
                    publish(CodexUsage(error = "Unable to read usage: ${e.message ?: "Unknown error"}"))
                }
            } finally {
                reading.set(false)
            }
        }
    }

    fun detectCodexPath(): String? = try {
        reader.detectCodexPath()
    } catch (e: Exception) {
        log.debug("Codex CLI was not found on PATH", e)
        null
    }

    private fun publish(usage: CodexUsage) {
        if (disposed) return
        val published = if (usage.error != null && (current.fiveHourLeft != null || current.weeklyLeft != null)) {
            current.copy(error = usage.error, plan = usage.plan ?: current.plan)
        } else usage
        if (current == published) return
        current = published
        ApplicationManager.getApplication().invokeLater {
            if (!disposed) listeners.forEach { listener ->
                try {
                    listener(published)
                } catch (e: Exception) {
                    log.warn("Unable to update Codex usage view", e)
                }
            }
        }
    }

    @Synchronized
    override fun dispose() {
        disposed = true
        scheduled?.cancel(false)
        reader.dispose()
        listeners.clear()
    }

    companion object {
        fun isAvailable(): Boolean {
            val configured = AgentsUsageSettings.getInstance().state.codexPath.trim()
            if (configured.isNotEmpty() && (configured.contains('\\') || configured.contains('/'))) {
                return File(configured).isFile
            }
            val names = if (configured.isBlank()) listOf("codex.exe", "codex.cmd", "codex.bat", "codex") else buildList {
                add(configured)
                if (File(configured).extension.isEmpty()) {
                    add("$configured.exe")
                    add("$configured.cmd")
                    add("$configured.bat")
                }
            }
            if (names.any { File(it).isFile }) return true
            val pathEntries = System.getenv("PATH")?.split(File.pathSeparator).orEmpty()
            return pathEntries.any { directory -> names.any { name -> File(directory, name).isFile } }
        }

        fun getInstance(): CodexUsageService =
            ApplicationManager.getApplication().getService(CodexUsageService::class.java)
    }
}
