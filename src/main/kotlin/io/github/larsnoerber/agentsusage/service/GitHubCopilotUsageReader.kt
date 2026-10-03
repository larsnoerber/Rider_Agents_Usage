package io.github.larsnoerber.agentsusage.service

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.PluginId
import io.github.larsnoerber.agentsusage.model.CopilotQuota
import io.github.larsnoerber.agentsusage.model.GitHubCopilotUsage
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.roundToInt

/** Reads Copilot's own quota service without taking a build dependency on its internal API. */
internal class GitHubCopilotUsageReader {
    private val api = PluginApi("GitHub Copilot")
    fun read(requestUpdate: Boolean): GitHubCopilotUsage {
        val plugin = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))
        if (plugin?.isEnabled != true) return GitHubCopilotUsage(error = "GitHub Copilot is not installed or enabled")
        val serviceClass = Class.forName("com.github.copilot.services.CopilotQuotaService", true, plugin.pluginClassLoader)
        val service = ApplicationManager.getApplication().getService(serviceClass)
            ?: error("GitHub Copilot quota service is unavailable")
        if (requestUpdate) getter(service, "refreshQuotaInBackground")
        val flow = getter(service, "getQuotaInfo") ?: error("GitHub Copilot quota state is unavailable")
        val state = api.flowValue(flow)
            ?: return GitHubCopilotUsage(error = "Waiting for GitHub Copilot usage. Sign in to Copilot.")
        val response = getter(state, "getQuotaResponse")
            ?: return GitHubCopilotUsage(error = "Waiting for GitHub Copilot usage. Sign in to Copilot.")
        val tokenBilling = getter(response, "getTokenBasedBillingEnabled", required = false) == true
        fun quota(name: String, title: String, showCounts: Boolean = true): CopilotQuota? {
            val value = getter(response, name, required = false) ?: return null
            // Copilot uses an EMPTY sentinel for categories that have not been reported yet.
            val companion = value.javaClass.getField("Companion").get(null)
            if (value == getter(companion, "getEMPTY")) return null
            val unlimited = getter(value, "getUnlimited") == true
            val percentage = (getter(value, "getPercentRemaining") as? Number)?.toDouble()
                ?.takeIf { it.isFinite() }?.coerceIn(0.0, 100.0)?.roundToInt()
            return CopilotQuota(
                title = title,
                percentLeft = if (unlimited) null else percentage,
                remaining = if (showCounts) (getter(value, "getQuotaRemaining", required = false) as? Number)?.toInt()?.coerceAtLeast(0) else null,
                total = if (showCounts) (getter(value, "getEntitlement", required = false) as? Number)?.toInt()?.coerceAtLeast(0) else null,
                unlimited = unlimited
            )
        }
        val premium = quota("getPremiumInteractions", if (tokenBilling) "AI credits" else "Premium requests", !tokenBilling)
        val chat = quota("getChat", "Chat")
        val completions = quota("getCompletions", "Completions")
        val primary = premium ?: chat ?: completions
        val reset = (getter(response, "getResetDateUtc", required = false) as? String)?.takeIf { it.isNotBlank() }
            ?: (getter(response, "getResetDate", required = false) as? String)
        return GitHubCopilotUsage(
            primary = primary,
            chat = chat,
            completions = completions,
            plan = (getter(response, "getCopilotPlan", required = false) as? String)?.takeIf { it.isNotBlank() },
            resetsAt = reset?.let(::parseReset),
            reportedAt = (getter(state, "getUpdatedAt", required = false) as? Instant)?.epochSecond,
            error = if (primary == null) "GitHub Copilot has not reported a usage quota yet" else null
        )
    }

    private fun parseReset(value: String): Long? = runCatching { Instant.parse(value).epochSecond }.getOrNull()
        ?: runCatching { LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toEpochSecond() }.getOrNull()

    private fun getter(target: Any, name: String, required: Boolean = true): Any? = api.getter(target, name, required)

    companion object {
        const val PLUGIN_ID = "com.github.copilot"
        fun isAvailable(): Boolean = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))?.isEnabled == true
    }
}
