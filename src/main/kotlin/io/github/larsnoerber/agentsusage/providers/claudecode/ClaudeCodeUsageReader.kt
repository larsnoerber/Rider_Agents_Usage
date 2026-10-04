package io.github.larsnoerber.agentsusage.providers.claudecode

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicBoolean
import io.github.larsnoerber.agentsusage.core.storage.LocalUsageFiles

/** Reads only token metadata from Claude Code's local session logs; prompt text is never retained. */
internal class ClaudeCodeUsageReader : AutoCloseable {
    private data class FileState(val size: Long, val modified: Long, val usage: ClaudeCodeUsage)

    private val closed = AtomicBoolean()
    private var cachedMonth: YearMonth? = null
    private val cache = mutableMapOf<Path, FileState>()

    fun read(): ClaudeCodeUsage {
        check(!closed.get()) { "Claude Code usage reader is closed" }
        val now = YearMonth.now()
        if (cachedMonth != now) {
            cache.clear()
            cachedMonth = now
        }
        val root = (LocalUsageFiles.absoluteEnvironment("CLAUDE_CONFIG_DIR") ?: LocalUsageFiles.home.resolve(".claude"))
            .resolve("projects")
        if (!Files.isDirectory(root)) return ClaudeCodeUsage()

        val cutoff = now.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val found = mutableSetOf<Path>()
        try {
            Files.walk(root).use { paths ->
                paths.takeWhile { !closed.get() }.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".jsonl") }
                    .forEach { path ->
                        val modified = runCatching { Files.getLastModifiedTime(path).toMillis() }.getOrDefault(0)
                        if (modified < cutoff && path !in cache) return@forEach
                        val size = runCatching { Files.size(path) }.getOrDefault(-1)
                        val normalized = path.toAbsolutePath().normalize()
                        found.add(normalized)
                        val previous = cache[normalized]
                        if (previous == null || previous.size != size || previous.modified != modified) {
                            cache[normalized] = FileState(size, modified, parseFile(path, now))
                        }
                    }
            }
        } catch (_: Exception) {
            // One unreadable folder should not hide usage already read from other session files.
        }
        cache.keys.retainAll(found)
        return cache.values.fold(ClaudeCodeUsage(logsFound = found.isNotEmpty())) { sum, state -> sum + state.usage }
            .copy(logsFound = found.isNotEmpty())
    }

    private fun parseFile(path: Path, month: YearMonth): ClaudeCodeUsage {
        val messages = linkedMapOf<String, ClaudeCodeUsage>()
        var anonymousEntries = 0
        try {
            Files.newBufferedReader(path).useLines { lines ->
                lines.takeWhile { !closed.get() }.forEach { line ->
                    val entry = runCatching { JsonParser.parseString(line).takeIf { it.isJsonObject }?.asJsonObject }.getOrNull()
                        ?: return@forEach
                    if (entry.get("type")?.asString != "assistant") return@forEach
                    val date = runCatching { Instant.parse(entry.get("timestamp")?.asString).atZone(ZoneId.systemDefault()) }.getOrNull()
                        ?: return@forEach
                    if (YearMonth.from(date) != month) return@forEach
                    val message = entry.get("message")?.takeIf { it.isJsonObject }?.asJsonObject ?: return@forEach
                    val usage = message.get("usage")?.takeIf { it.isJsonObject }?.asJsonObject ?: return@forEach
                    val messageId = message.get("id")?.takeIf { it.isJsonPrimitive }?.asString
                    val model = message.get("model")?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() } ?: "Unknown model"
                    val input = usage.long("input_tokens")
                    val output = usage.long("output_tokens")
                    val cacheRead = usage.long("cache_read_input_tokens")
                    val cacheWrite = usage.long("cache_creation_input_tokens")
                    // Streamed entries repeat a message ID. Retain its final usage instead of counting every chunk.
                    messages[messageId ?: "local-entry-${anonymousEntries++}"] = ClaudeCodeUsage(
                        requests = 1,
                        inputTokens = input,
                        outputTokens = output,
                        cacheReadTokens = cacheRead,
                        cacheWriteTokens = cacheWrite,
                        modelTokens = mapOf(model to (input + output + cacheRead + cacheWrite)),
                        logsFound = true
                    )
                }
            }
        } catch (_: Exception) {
            // Skip an unreadable or partially written log without exposing its contents.
        }
        return messages.values.fold(ClaudeCodeUsage(logsFound = true)) { sum, usage -> sum + usage }
    }

    override fun close() {
        closed.set(true)
    }

    private fun JsonObject.long(name: String): Long = get(name)
        ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
        ?.let { runCatching { it.asLong.coerceAtLeast(0) }.getOrDefault(0) } ?: 0

    private operator fun ClaudeCodeUsage.plus(other: ClaudeCodeUsage): ClaudeCodeUsage = ClaudeCodeUsage(
        requests + other.requests,
        inputTokens + other.inputTokens,
        outputTokens + other.outputTokens,
        cacheReadTokens + other.cacheReadTokens,
        cacheWriteTokens + other.cacheWriteTokens,
        (modelTokens.keys + other.modelTokens.keys).associateWith { (modelTokens[it] ?: 0) + (other.modelTokens[it] ?: 0) },
        logsFound || other.logsFound
    )
}
