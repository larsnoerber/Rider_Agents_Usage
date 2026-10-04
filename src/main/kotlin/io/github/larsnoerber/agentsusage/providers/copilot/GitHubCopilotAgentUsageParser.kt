package io.github.larsnoerber.agentsusage.providers.copilot

import com.google.gson.JsonObject
import io.github.larsnoerber.agentsusage.core.storage.numberValue
import io.github.larsnoerber.agentsusage.core.storage.objectValue
import io.github.larsnoerber.agentsusage.core.storage.stringValue
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.roundToInt

/** Maps the Copilot Language Server's checkQuota response to the existing consumption UI. */
internal object GitHubCopilotAgentUsageParser {
    fun parse(response: JsonObject): GitHubCopilotUsage {
        val plan = response.stringValue("copilotPlan")
        val freePlan = plan.equals("free", ignoreCase = true)
        val tokenBilling = response.get("tokenBasedBillingEnabled")?.asBoolean == true
        fun quota(key: String, title: String): CopilotQuota? {
            val value = response.objectValue(key) ?: return null
            val unlimited = value.get("unlimited")?.asBoolean == true
            val entitlement = value.numberValue("entitlement")?.takeIf { it.isFinite() }
            val remaining = value.numberValue("quotaRemaining")?.takeIf { it.isFinite() }
            if (!unlimited && entitlement == 0.0 && (remaining == null || remaining == 0.0)) return null
            val percent = value.numberValue("percentRemaining")?.takeIf { it.isFinite() }
            if (!unlimited && percent == null) return null
            return CopilotQuota(title, if (unlimited) null else percent?.coerceIn(0.0, 100.0)?.roundToInt(),
                remaining = if (tokenBilling || unlimited) null else remaining?.coerceAtLeast(0.0)?.roundToInt(),
                total = if (tokenBilling || unlimited) null else entitlement?.coerceAtLeast(0.0)?.roundToInt(),
                unlimited = unlimited)
        }
        val premium = if (freePlan) null else quota("premiumInteractions", if (tokenBilling) "AI credits" else "Premium requests")
        val chat = quota("chat", if (freePlan && tokenBilling) "AI credits" else "Chat")
        val completions = quota("completions", "Completions")
        val primary = if (freePlan) chat ?: completions else premium
        val reset = response.stringValue("resetDateUtc")?.takeIf { it.isNotBlank() }
            ?: response.stringValue("resetDate")
        return GitHubCopilotUsage(primary = primary, chat = chat, completions = completions, plan = plan,
            resetsAt = reset?.let { value ->
                runCatching { Instant.parse(value).epochSecond }.getOrNull()
                    ?: runCatching { LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toEpochSecond() }.getOrNull()
            }, reportedAt = Instant.now().epochSecond,
            error = if (primary == null) "GitHub Copilot has not reported an account quota yet." else null)
    }
}
