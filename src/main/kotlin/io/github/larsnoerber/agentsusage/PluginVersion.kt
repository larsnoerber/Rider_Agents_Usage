package io.github.larsnoerber.agentsusage

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId

/** Reads this plugin's version from the descriptor registered by the running IDE. */
object PluginVersion {
    val current: String by lazy {
        PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))?.version ?: "Unknown"
    }

    private const val PLUGIN_ID = "io.github.larsnoerber.agentsusage"
}
