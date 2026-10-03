package io.github.larsnoerber.agentsusage.core.reflection

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId

/** Find only loaded plugins; installed but disabled plugins cannot expose their quota service. */
internal fun loadedPlugin(id: String): IdeaPluginDescriptor? {
    val pluginId = PluginId.getId(id)
    return PluginManagerCore.getPlugin(pluginId)?.takeIf { PluginManagerCore.isLoaded(pluginId) }
}
