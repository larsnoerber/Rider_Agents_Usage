package io.github.larsnoerber.agentsusage.providers.jetbrainsai

import io.github.larsnoerber.agentsusage.core.reflection.PluginApi

/** Reads only the active license's product metadata, never account data or credentials. */
internal class JetBrainsAiSubscriptionReader {
    private val api = PluginApi("JetBrains AI subscription")

    fun read(pluginLoader: ClassLoader?): String? =
        optional { readActiveLicense(pluginLoader) } ?: optional { readAuthPlan(pluginLoader) }

    private fun readActiveLicense(pluginLoader: ClassLoader?): String? {
        val manager = service("$ACTIVATION_PACKAGE.state.manager.AiaActivationManager", pluginLoader)
        val activation = getter(manager, "getJbaiActivation") ?: return null

        // The new AI Access activation reports JbaiOther in the auth facade even for a named subscription.
        // Its selected mode retains the product's display name and is the source used by the license picker.
        val access = getter(activation, "getAccessState")
        if (access != null) {
            val mode = getter(access, "getAccessMode") ?: return null
            return when (mode.javaClass.simpleName) {
                "JcpLicense" -> getter(mode, "getReported")?.let { reported ->
                    (getter(reported, "getDisplayName") as? String)?.trim()?.takeIf { it.isNotEmpty() }
                }
                "LegacyLicense" -> getter(mode, "getLicense")?.let(::legacyPlan)
                else -> null // A workspace does not report a personal subscription tier here.
            }
        }

        // Older activation exposes the selected license directly. Do not choose from available licenses.
        return getter(activation, "getActiveLicense")?.let(::legacyPlan)
    }

    private fun legacyPlan(license: Any): String? {
        val product = (getter(license, "getProductType") as? Enum<*>)?.name ?: return null
        val plan = when (product) {
            "AIPU" -> "AI Ultimate"
            "AIP" -> "AI Pro"
            "AIF" -> "AI Free"
            "AIPEAP" -> "AI EAP"
            else -> return null
        }
        return when {
            getter(license, "isTrial") == true -> "$plan (Trial)"
            getter(license, "isPack") == true -> "$plan (included)"
            else -> plan
        }
    }

    private fun readAuthPlan(pluginLoader: ClassLoader?): String? {
        val facade = service("$ACTIVATION_PACKAGE.facade.AiaActivationAuthFacade", pluginLoader)
        val idsClass = Class.forName("$ACTIVATION_PACKAGE.data.model.LlmProtocolIds", true, pluginLoader)
        val protocol = getter(idsClass.getField("INSTANCE").get(null), "getGrazie") ?: return null
        val authFor = facade.javaClass.methods.firstOrNull {
            it.parameterCount == 1 && (it.name == "authFor" || it.name.startsWith("authFor-"))
        } ?: return null
        if (!authFor.canAccess(facade)) authFor.trySetAccessible()
        val holder = authFor.invoke(facade, protocol) ?: return null
        val state = getter(holder, "getCurrentAuthState") ?: return null
        val providers = getter(state, "getAuthList") as? Iterable<*> ?: return null
        // Skip the generic Other marker if another auth entry has a concrete tier.
        return providers.firstNotNullOfOrNull { auth ->
            val data = auth?.let { getter(it, "getProviderData") }
            val plan = data?.let { getter(it, "getSubscriptionPlan") as? Enum<*> }?.name
            when (plan) {
                "JbaiFree" -> "AI Free"
                "JbaiPro" -> "AI Pro"
                "JbaiProTrial" -> "AI Pro (Trial)"
                "JbaiProPack" -> "AI Pro (included)"
                "JbaiUltimate" -> "AI Ultimate"
                "JbaiEnterprise" -> "AI Enterprise"
                "JbaiEap" -> "AI EAP"
                "JbaiOther", null -> null
                else -> plan
            }
        }
    }

    private fun service(name: String, loader: ClassLoader?): Any {
        val type = Class.forName(name, true, loader)
        val companion = type.getField("Companion").get(null)
        return getter(companion, "getInstance") ?: error("JetBrains AI activation service is unavailable")
    }

    private fun getter(target: Any, name: String): Any? = api.getter(target, name, required = false)

    private fun optional(read: () -> String?): String? = try {
        read()
    } catch (_: Exception) {
        null
    } catch (_: LinkageError) {
        null
    }

    private companion object {
        const val ACTIVATION_PACKAGE = "com.intellij.ml.llm.activation"
    }
}
