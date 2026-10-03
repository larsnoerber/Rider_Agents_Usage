package io.github.larsnoerber.agentsusage.core.reflection

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore

/** Find only loaded plugins; installed but disabled plugins cannot expose their quota service. */
internal fun loadedPlugin(id: String): IdeaPluginDescriptor? =
    PluginManagerCore.loadedPlugins.firstOrNull { it.pluginId.idString == id }
