package io.github.larsnoerber.agentsusage.core.reflection

import com.intellij.ide.plugins.PluginManagerCore

/** New platform versions isolate content modules from their parent plugin's class loader. */
internal fun loadPluginClass(name: String, pluginLoader: ClassLoader?, moduleName: String): Class<*> {
    try {
        return Class.forName(name, true, pluginLoader)
    } catch (_: ClassNotFoundException) {
        val idClass = Class.forName("com.intellij.ide.plugins.PluginModuleId")
        val id = idClass.getMethod("getId", String::class.java, String::class.java)
            .invoke(null, moduleName, idClass.getField("JETBRAINS_NAMESPACE").get(null))
        val plugins = PluginManagerCore::class.java.getMethod("getPluginSet").invoke(null)
        val module = plugins.javaClass.getMethod("findEnabledModule", idClass).invoke(plugins, id)
            ?: error("Optional plugin module is unavailable: $moduleName")
        val loader = module.javaClass.getMethod("getPluginClassLoader").invoke(module) as? ClassLoader
            ?: error("Optional plugin module has no class loader: $moduleName")
        return Class.forName(name, true, loader)
    }
}
