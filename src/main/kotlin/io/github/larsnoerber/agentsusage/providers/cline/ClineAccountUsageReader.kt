package io.github.larsnoerber.agentsusage.providers.cline

import io.github.larsnoerber.agentsusage.core.http.UsageHttp
import io.github.larsnoerber.agentsusage.core.http.UsageHttpFailure
import io.github.larsnoerber.agentsusage.core.storage.LocalUsageFiles
import io.github.larsnoerber.agentsusage.core.storage.numberValue
import io.github.larsnoerber.agentsusage.core.storage.objectValue
import io.github.larsnoerber.agentsusage.core.storage.stringValue
import java.net.URLEncoder
import java.nio.file.Files

/** Reuse the official CLI/ACP provider store only for Cline's own account request. */
internal class ClineAccountUsageReader : AutoCloseable {
    private val http = UsageHttp()

    fun read(): ClineUsage {
        return try {
            if (!Files.isRegularFile(credentialsFile())) return ClineUsage(accountError = "No local Cline account sign-in found.")
            val auth = LocalUsageFiles.json(credentialsFile()).objectValue("providers")?.objectValue("cline")
                ?.objectValue("settings")?.objectValue("auth")
                ?: return ClineUsage(accountError = "No Cline account sign-in found; other model-provider logins do not expose Cline credits.")
            val token = auth.stringValue("accessToken") ?: return ClineUsage(accountError = "Cline account session unavailable.")
            if (auth.numberValue("expiresAt")?.let { it <= System.currentTimeMillis() } == true)
                return ClineUsage(accountError = "Cline session expired. Open the Cline agent to renew your sign-in.")
            val headers = mapOf("Authorization" to "Bearer $token")
            val userId = auth.objectValue("metadata")?.objectValue("userInfo")?.stringValue("clineUserId")
                ?: payload(http.get("https://api.cline.bot/api/v1/users/me", headers)).stringValue("id")
                ?: return ClineUsage(accountError = "Cline did not report an account for this login.")
            val response = payload(http.get("https://api.cline.bot/api/v1/users/${URLEncoder.encode(userId, Charsets.UTF_8)}/balance", headers))
            // The official Cline Hub formats this signed balance as USD by dividing by 1e6.
            val balance = response.get("balance")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
                ?.asDouble?.takeIf(Double::isFinite)
                ?: return ClineUsage(accountError = "Cline did not report an account balance.")
            ClineUsage(accountBalanceUsd = balance / 1_000_000.0, updatedAt = System.currentTimeMillis())
        } catch (failure: UsageHttpFailure) {
            ClineUsage(accountError = when (failure.status) {
                401, 403 -> "Cline rejected the account session. Reconnect the Cline agent."
                429 -> "Cline account usage is rate limited. Try again later."
                else -> "Cline account request failed (HTTP ${failure.status})."
            })
        } catch (_: Exception) { ClineUsage(accountError = "Cline account balance unavailable. Check your connection and agent sign-in.") }
    }

    private fun payload(response: com.google.gson.JsonObject) = response.objectValue("data") ?: response

    override fun close() = http.close()

    companion object {
        private fun credentialsFile() = (LocalUsageFiles.absoluteEnvironment("CLINE_DATA_DIR")
            ?: (LocalUsageFiles.absoluteEnvironment("CLINE_DIR") ?: LocalUsageFiles.home.resolve(".cline")).resolve("data"))
            .resolve("settings/providers.json")

        fun hasLocalAccount(): Boolean = Files.isRegularFile(credentialsFile()) && runCatching {
            LocalUsageFiles.json(credentialsFile()).objectValue("providers")?.objectValue("cline")
                ?.objectValue("settings")?.objectValue("auth")?.has("accessToken") == true
        }.getOrDefault(false)
    }
}
