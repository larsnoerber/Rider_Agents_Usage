package io.github.larsnoerber.agentsusage.providers.codex

import com.google.gson.JsonObject
import io.github.larsnoerber.agentsusage.core.format.formatResetTime
import kotlin.math.roundToInt

object CodexUsageParser {
    fun parseRateLimits(response: JsonObject): CodexUsage {
        val result = response.objectValue("result")
            ?: return CodexUsage(error = "Codex app-server did not return usage data")
        val snapshot = result.objectValue("rateLimitsByLimitId")?.objectValue("codex")
            ?: result.objectValue("rateLimits")
            ?: return CodexUsage(error = "Codex app-server did not return usage data")
        val primary = snapshot.objectValue("primary")
        val secondary = snapshot.objectValue("secondary")
        fun remaining(window: JsonObject?): Int? = window?.stringValue("usedPercent")
            ?.toDoubleOrNull()?.takeIf(Double::isFinite)?.coerceIn(0.0, 100.0)?.roundToInt()?.let { 100 - it }
        fun reset(window: JsonObject?): Long? = window?.stringValue("resetsAt")?.toLongOrNull()
        fun displayReset(value: Long?): String? = value?.let { formatResetTime(it) }
        val fiveLeft = remaining(primary)
        val weekLeft = remaining(secondary)
        val fiveReset = reset(primary)
        val weekReset = reset(secondary)
        return CodexUsage(
            fiveHourLeft = fiveLeft,
            fiveHourReset = displayReset(fiveReset),
            fiveHourResetsAt = fiveReset,
            weeklyLeft = weekLeft,
            weeklyReset = displayReset(weekReset),
            weeklyResetsAt = weekReset,
            credits = snapshot.objectValue("credits")?.stringValue("balance")?.toIntOrNull(),
            plan = snapshot.stringValue("planType") ?: result.objectValue("rateLimits")?.stringValue("planType"),
            error = if (fiveLeft == null && weekLeft == null) "No 5-hour or weekly quota was found" else null
        )
    }

}
