package io.github.larsnoerber.agentsusage.core

/** UI-facing contract. Implementations read in the background and notify listeners on the EDT. */
interface UsageSource<T> {
    val current: T
    fun addListener(listener: (T) -> Unit)
    fun removeListener(listener: (T) -> Unit)
    fun refresh()
}
