package io.github.larsnoerber.agentsusage.providers.jetbrainsai

import com.intellij.openapi.application.ApplicationManager
import io.github.larsnoerber.agentsusage.core.reflection.PluginApi
import io.github.larsnoerber.agentsusage.core.reflection.loadPluginClass
import java.lang.reflect.InvocationTargetException

/** Reads only the active license's product metadata, never account data or credentials. */
internal class JetBrainsAiSubscriptionReader {
    private val api = PluginApi("JetBrains AI subscription")
    private val lookupNotes = mutableListOf<String>()
    var unavailableReason: String? = null
        private set

    fun read(pluginLoader: ClassLoader?): String? {
        lookupNotes.clear()
        val plan = optional("Active license") { readActiveLicense(pluginLoader) }
            ?: optional("Subscription tier") { readAuthPlan(pluginLoader) }
        unavailableReason = if (plan != null) null else lookupNotes.distinct().joinToString("; ")
            .ifEmpty { "AI Assistant has not reported a subscription name" }
        return plan
    }

    private fun readActiveLicense(pluginLoader: ClassLoader?): String? {
        val manager = service("$ACTIVATION_PACKAGE.state.manager.AiaActivationManager", pluginLoader)
        // The provider publishes an immutable activation snapshot for consumers outside its Compose UI.
        val snapshot = optional("Activation snapshot") {
            getter(manager, "getActivationState")?.let(api::flowValue)?.let { getter(it, "getJbaiActivation") }
        }
        val activation = snapshot ?: getter(manager, "getJbaiActivation") ?: return null

        // The new AI Access activation reports JbaiOther in the auth facade even for a named subscription.
        // Its selected mode retains the product's display name and is the source used by the license picker.
        val access = getter(activation, "getAccessState")
        if (access != null) {
            val mode = getter(access, "getAccessMode") ?: return null
            return when (mode.javaClass.simpleName) {
                "JcpLicense" -> getter(mode, "getReported")?.let { reported ->
                    (getter(reported, "getDisplayName") as? String)?.trim()?.takeIf { it.isNotEmpty() }
                        ?: productPlan((getter(reported, "getProductCode") as? String).orEmpty())
                }
                "LegacyLicense" -> getter(mode, "getLicense")?.let(::legacyPlan)
                "JcpWorkspace" -> if (getter(mode, "getAiAccessEnabled") == true) "AI Workspace" else null
                else -> null.also { lookupNotes += "Active access type: ${mode.javaClass.simpleName}" }
            }
        }

        // Older activation exposes the selected license directly. Do not choose from available licenses.
        val license = getter(activation, "getActiveLicense")
            ?: getter(activation, "getLicenseJourney")?.let { getter(it, "getActiveLicense") }
        return license?.let(::legacyPlan)
    }

    private fun legacyPlan(license: Any): String? {
        val product = (getter(license, "getProductType") as? Enum<*>)?.name ?: return null
        val plan = productPlan(product) ?: return null
        return when {
            getter(license, "isTrial") == true -> "$plan (Trial)"
            getter(license, "isPack") == true -> "$plan (included)"
            else -> plan
        }
    }

    private fun productPlan(product: String): String? = when (product) {
        "AIPU" -> "AI Ultimate"
        "AIP" -> "AI Pro"
        "AIF" -> "AI Free"
        "AIPEAP" -> "AI EAP"
        else -> null
    }

    private fun readAuthPlan(pluginLoader: ClassLoader?): String? {
        val facade = service("$ACTIVATION_PACKAGE.facade.AiaActivationAuthFacade", pluginLoader)
        val idsClass = loadPluginClass("$ACTIVATION_PACKAGE.data.model.LlmProtocolIds", pluginLoader,
            "intellij.ml.llm.activation.data.model")
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
                "JbaiOther", null -> null.also { lookupNotes += "No named subscription tier reported" }
                else -> plan
            }
        }
    }

    private fun service(name: String, loader: ClassLoader?): Any {
        val type = loadPluginClass(name, loader, name.substringBeforeLast('.').replace("com.intellij", "intellij"))
        // These interfaces are registered as application services by AI Assistant. Prefer their IDE service
        // instances over the newer Compose service-locator facade, which can depend on provider context.
        optional("${type.simpleName} application service") {
            ApplicationManager.getApplication().getService(type)
        }?.let { return it }
        val companion = type.getField("Companion").get(null)
        return getter(companion, "getInstance") ?: error("JetBrains AI activation service is unavailable")
    }

    private fun getter(target: Any, name: String): Any? = api.getter(target, name, required = false).also {
        if (it == null) lookupNotes += "Unavailable metadata: ${target.javaClass.simpleName}.$name"
    }

    private fun <T> optional(stage: String, read: () -> T?): T? = try {
        read()
    } catch (error: Exception) {
        val cause = if (error is InvocationTargetException) error.targetException else error
        // Class/getter names and exception types only. Provider messages and object contents may contain
        // account data, so they must never be included in diagnostics.
        lookupNotes += "$stage: ${cause.javaClass.simpleName}"
        null
    } catch (error: LinkageError) {
        lookupNotes += "$stage: ${error.javaClass.simpleName}"
        null
    }

    private companion object {
        const val ACTIVATION_PACKAGE = "com.intellij.ml.llm.activation"
    }
}
