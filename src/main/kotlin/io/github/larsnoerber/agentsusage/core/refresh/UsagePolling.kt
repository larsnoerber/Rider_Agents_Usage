package io.github.larsnoerber.agentsusage.core.refresh

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Polls local plugin state frequently while respecting the shared server refresh interval. */
internal class UsagePolling<T>(
    initial: T,
    private val read: (Boolean) -> T,
    private val refreshIntervalSeconds: () -> Int,
    private val enabled: () -> Boolean = { true },
    private val failure: (Throwable) -> T
) : Disposable {
    private val listeners = CopyOnWriteArrayList<(T) -> Unit>()
    private val reading = AtomicBoolean()
    private val forceRefresh = AtomicBoolean()
    @Volatile private var disposed = false
    private var lastRefreshNanos: Long? = null
    @Volatile var current: T = initial
        private set
    private val scheduled = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
        { poll() }, 0, 5, TimeUnit.SECONDS
    )

    fun addListener(listener: (T) -> Unit) { listeners.addIfAbsent(listener) }
    fun removeListener(listener: (T) -> Unit) { listeners -= listener }

    fun refresh() {
        if (disposed || !enabled() || !forceRefresh.compareAndSet(false, true)) return
        ApplicationManager.getApplication().executeOnPooledThread { poll() }
    }

    private fun poll() {
        if (disposed || !enabled() || !reading.compareAndSet(false, true)) return
        try {
            val now = System.nanoTime()
            val interval = TimeUnit.SECONDS.toNanos(refreshIntervalSeconds().toLong())
            val previous = lastRefreshNanos
            val requestUpdate = forceRefresh.getAndSet(false) || previous == null || now - previous >= interval
            val usage = try {
                read(requestUpdate)
            } catch (e: Exception) {
                failure(e)
            } catch (e: LinkageError) {
                failure(e)
            }
            if (requestUpdate) lastRefreshNanos = now
            if (disposed || current == usage) return
            current = usage
            ApplicationManager.getApplication().invokeLater {
                if (!disposed) listeners.forEach { listener ->
                    try {
                        listener(usage)
                    } catch (e: Exception) {
                        Logger.getInstance(UsagePolling::class.java).warn("Unable to update usage view", e)
                    }
                }
            }
        } finally {
            reading.set(false)
        }
    }

    override fun dispose() {
        disposed = true
        scheduled.cancel(false)
        listeners.clear()
    }
}
