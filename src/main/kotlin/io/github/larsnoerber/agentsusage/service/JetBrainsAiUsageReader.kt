package io.github.larsnoerber.agentsusage.service

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.PluginId
import io.github.larsnoerber.agentsusage.model.JetBrainsAiQuota
import io.github.larsnoerber.agentsusage.model.JetBrainsAiUsage
import java.math.BigDecimal
import java.time.Instant

/** Keeps the optional, internal AI Assistant API isolated from the rest of the plugin. */
internal class JetBrainsAiUsageReader {
    private val api = PluginApi("JetBrains AI")
    fun read(requestUpdate: Boolean): JetBrainsAiUsage {
        val plugin = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))
        if (plugin?.isEnabled != true) return JetBrainsAiUsage(error = "JetBrains AI Assistant is not installed or enabled")
        val plan = readSubscriptionPlan(plugin.pluginClassLoader)
        val managerClass = loadManagerClass(plugin.pluginClassLoader)
        val manager = ApplicationManager.getApplication().getService(managerClass)
            ?: error("JetBrains AI quota service is unavailable")
        if (requestUpdate) getter(manager, "requestUpdateEverything")
        val info = flowValue(getter(manager, "getQuotaInfo") ?: error("JetBrains AI quota is unavailable"))
            ?: return JetBrainsAiUsage(plan = plan, error = "Waiting for JetBrains AI quota. Sign in to AI Assistant.")
        val refill = getter(manager, "getNextRefill")?.let(::flowValue)
        val reset = refill?.let { getter(it, "getNext", required = false) }?.toString()
            ?.let { runCatching { Instant.parse(it).epochSecond }.getOrNull() }
        return when (info.javaClass.simpleName) {
            "Unlimited" -> JetBrainsAiUsage(unlimited = true, plan = plan)
            "Available", "Reached" -> {
                // `current` is consumed quota; `available` in the details is the remaining balance.
                val maximum = amount(info, "getMaximum") ?: error("JetBrains AI quota format is unsupported")
                val used = if (info.javaClass.simpleName == "Reached") maximum else
                    amount(info, "getCurrent") ?: error("JetBrains AI quota format is unsupported")
                val unitsPerCredit = managerClass.classLoader.loadClass("$QUOTA_PACKAGE.CreditsKt")
                    .getMethod("getUNITS_IN_CREDIT").invoke(null) as BigDecimal
                fun quota(remaining: BigDecimal, total: BigDecimal) = JetBrainsAiQuota(
                    remaining.max(BigDecimal.ZERO).divide(unitsPerCredit),
                    total.max(BigDecimal.ZERO).divide(unitsPerCredit)
                )
                fun details(name: String): JetBrainsAiQuota? {
                    val detail = getter(info, name, required = false) ?: return null
                    val total = amount(detail, "getMaximum") ?: return null
                    val remaining = amount(detail, "getAvailable")
                        ?: amount(detail, "getCurrent")?.let { total.subtract(it) } ?: return null
                    return quota(remaining, total)
                }
                JetBrainsAiUsage(
                    quota = quota(maximum.subtract(used), maximum),
                    subscription = details("getTariffQuota"),
                    topUp = details("getTopUpQuota"),
                    resetsAt = reset,
                    plan = plan
                )
            }
            "Error" -> JetBrainsAiUsage(plan = plan, error = "JetBrains AI could not refresh the credit balance. Check AI Assistant.")
            else -> JetBrainsAiUsage(plan = plan, error = "Waiting for JetBrains AI quota. Sign in to AI Assistant.")
        }
    }

    private fun readSubscriptionPlan(pluginLoader: ClassLoader?): String? = try {
        // Read only the provider's subscription metadata, never credentials or auth headers.
        val facadeClass = Class.forName("com.intellij.ml.llm.activation.facade.AiaActivationAuthFacade", true, pluginLoader)
        val companion = facadeClass.getField("Companion").get(null)
        val facade = getter(companion, "getInstance") ?: error("AI activation is unavailable")
        val idsClass = Class.forName("com.intellij.ml.llm.activation.data.model.LlmProtocolIds", true, pluginLoader)
        val protocol = getter(idsClass.getField("INSTANCE").get(null), "getGrazie")
        val authFor = facadeClass.methods.first {
            it.parameterCount == 1 && (it.name == "authFor" || it.name.startsWith("authFor-"))
        }
        val holder = authFor.invoke(facade, protocol)
        val state = getter(holder, "getCurrentAuthState") ?: error("AI activation is unavailable")
        val providers = getter(state, "getAuthList") as? Iterable<*> ?: emptyList<Any>()
        val reportedPlan = providers.firstNotNullOfOrNull { auth ->
            auth?.let { getter(it, "getProviderData", required = false) }
                ?.let { getter(it, "getSubscriptionPlan", required = false) as? Enum<*> }?.name
        }
        when (reportedPlan) {
            "JbaiFree" -> "AI Free"
            "JbaiPro" -> "AI Pro"
            "JbaiProTrial" -> "AI Pro (Trial)"
            "JbaiProPack" -> "AI Pro (included)"
            "JbaiUltimate" -> "AI Ultimate"
            "JbaiEnterprise" -> "AI Enterprise"
            "JbaiEap" -> "AI EAP"
            "JbaiOther" -> "Other"
            else -> reportedPlan
        }
    } catch (_: Exception) {
        null
    } catch (_: LinkageError) {
        null
    }

    private fun loadManagerClass(pluginLoader: ClassLoader?): Class<*> {
        val name = "$QUOTA_PACKAGE.QuotaManager2"
        try {
            return Class.forName(name, true, pluginLoader)
        } catch (_: ClassNotFoundException) {
            // New IDE versions give AI Assistant content modules their own class loaders.
            val moduleIdClass = Class.forName("com.intellij.ide.plugins.PluginModuleId")
            val moduleId = moduleIdClass.getMethod("getId", String::class.java, String::class.java)
                .invoke(null, "intellij.ml.llm.core", moduleIdClass.getField("JETBRAINS_NAMESPACE").get(null))
            val pluginSet = PluginManagerCore::class.java.getMethod("getPluginSet").invoke(null)
            val module = pluginSet.javaClass.getMethod("findEnabledModule", moduleIdClass).invoke(pluginSet, moduleId)
                ?: error("JetBrains AI quota module is unavailable")
            val loader = module.javaClass.getMethod("getPluginClassLoader").invoke(module) as ClassLoader
            return Class.forName(name, true, loader)
        }
    }

    private fun flowValue(flow: Any): Any? = api.flowValue(flow)

    private fun getter(target: Any, name: String, required: Boolean = true): Any? = api.getter(target, name, required)

    private fun amount(target: Any, name: String): BigDecimal? =
        when (val value = getter(target, name, required = false)) {
            is BigDecimal -> value
            is Number -> value.toString().toBigDecimalOrNull()
            else -> null
        }

    companion object {
        const val PLUGIN_ID = "com.intellij.ml.llm"
        private const val QUOTA_PACKAGE = "com.intellij.ml.llm.core.quota"
        fun isAvailable(): Boolean = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))?.isEnabled == true
    }
}
