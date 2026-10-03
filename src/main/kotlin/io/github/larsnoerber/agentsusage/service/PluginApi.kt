package io.github.larsnoerber.agentsusage.service

import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

/** Caches optional API getters per runtime class, including missing getters. */
internal class PluginApi(private val provider: String) {
    private data class Lookup(val method: Method?)
    private val getters = object : ClassValue<ConcurrentHashMap<String, Lookup>>() {
        override fun computeValue(type: Class<*>): ConcurrentHashMap<String, Lookup> = ConcurrentHashMap()
    }

    fun getter(target: Any, name: String, required: Boolean = true): Any? {
        val lookup = getters.get(target.javaClass).computeIfAbsent(name) {
            // Kotlin value classes can add a mangled suffix to a getter name.
            Lookup(target.javaClass.methods.firstOrNull {
                it.parameterCount == 0 && (it.name == name || it.name.startsWith("$name-"))
            })
        }
        val method = lookup.method ?: if (required) error("Unsupported $provider API: $name") else return null
        if (!method.canAccess(target)) method.trySetAccessible()
        return method.invoke(target)
    }

    fun flowValue(flow: Any): Any? = getter(flow, "getValue")
}
