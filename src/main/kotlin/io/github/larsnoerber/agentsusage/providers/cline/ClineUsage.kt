package io.github.larsnoerber.agentsusage.providers.cline

data class ClineUsage(
    val tasks: Int = 0,
    val inputTokens: Long = 0,
    val outputTokens: Long = 0,
    val cacheReadTokens: Long = 0,
    val cacheWriteTokens: Long = 0,
    val costUsd: Double? = null,
    val sources: Int = 0,
    val updatedAt: Long = 0,
    val error: String? = null,
    val accountBalanceUsd: Double? = null,
    val accountError: String? = null
) {
    val totalTokens: Long get() = inputTokens + outputTokens
}
