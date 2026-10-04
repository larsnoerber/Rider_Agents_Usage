package io.github.larsnoerber.agentsusage.providers.cline

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import io.github.larsnoerber.agentsusage.core.storage.LocalUsageFiles
import io.github.larsnoerber.agentsusage.core.storage.numberValue
import io.github.larsnoerber.agentsusage.core.storage.objectValue
import io.github.larsnoerber.agentsusage.core.storage.stringValue
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

/** Reads Cline's task aggregates and SDK message metrics, without retaining message or task text. */
internal class ClineUsageReader : AutoCloseable {
    private val closed = AtomicBoolean()

    companion object {
        fun hasLocalUsageSource(): Boolean {
            val clineRoot = LocalUsageFiles.absoluteEnvironment("CLINE_DIR") ?: LocalUsageFiles.home.resolve(".cline")
            val dataRoot = LocalUsageFiles.absoluteEnvironment("CLINE_DATA_DIR") ?: clineRoot.resolve("data")
            val roots = listOf(dataRoot, clineRoot) + listOf("Code", "Code - Insiders", "Cursor", "Windsurf").map {
                LocalUsageFiles.configRoot(it).resolve("User/globalStorage/saoudrizwan.claude-dev")
            }
            val historyOrDatabase = roots.any { root ->
                Files.isRegularFile(root.resolve("state/taskHistory.json")) ||
                    Files.isRegularFile(root.resolve("db/sessions.db")) ||
                    Files.isRegularFile(root.resolve("data/db/sessions.db"))
            }
            val customDatabaseRoot = LocalUsageFiles.absoluteEnvironment("CLINE_DB_DATA_DIR")
            return historyOrDatabase || customDatabaseRoot?.let { Files.isRegularFile(it.resolve("sessions.db")) } == true
        }
    }

    fun read(): ClineUsage {
        check(!closed.get())
        val clineRoot = LocalUsageFiles.absoluteEnvironment("CLINE_DIR") ?: LocalUsageFiles.home.resolve(".cline")
        val dataRoot = LocalUsageFiles.absoluteEnvironment("CLINE_DATA_DIR") ?: clineRoot.resolve("data")
        val roots = (listOf(dataRoot, clineRoot) + listOf("Code", "Code - Insiders", "Cursor", "Windsurf").map {
            LocalUsageFiles.configRoot(it).resolve("User/globalStorage/saoudrizwan.claude-dev")
        }).map { it.toAbsolutePath().normalize() }.distinct()
        val seen = mutableSetOf<String>()
        var result = ClineUsage()
        var unreadable = false
        var sources = 0
        fun add(id: String, usage: ClineUsage) {
            if (!seen.add(id)) return
            result = result.copy(tasks = result.tasks + 1,
                inputTokens = result.inputTokens + usage.inputTokens, outputTokens = result.outputTokens + usage.outputTokens,
                cacheReadTokens = result.cacheReadTokens + usage.cacheReadTokens,
                cacheWriteTokens = result.cacheWriteTokens + usage.cacheWriteTokens,
                costUsd = if (result.costUsd == null && usage.costUsd == null) null else (result.costUsd ?: 0.0) + (usage.costUsd ?: 0.0))
        }
        roots.forEach { root ->
            if (closed.get()) return@forEach
            val history = root.resolve("state/taskHistory.json")
            if (Files.isRegularFile(history)) {
                try {
                    arrayObjects(history) { task ->
                        val id = task.stringValue("id") ?: return@arrayObjects
                        if (task.numberValue("tokensIn") == null && task.numberValue("tokensOut") == null) return@arrayObjects
                        add(id, ClineUsage(inputTokens = task.count("tokensIn"), outputTokens = task.count("tokensOut"),
                            cacheReadTokens = task.count("cacheReads"), cacheWriteTokens = task.count("cacheWrites"),
                            costUsd = task.numberValue("totalCost")))
                    }
                    sources++
                } catch (_: Exception) { unreadable = true }
            }
        }
        val dbRoots = (roots.flatMap { listOf(it.resolve("db"), it.resolve("data/db")) } +
            listOfNotNull(LocalUsageFiles.absoluteEnvironment("CLINE_DB_DATA_DIR"))).distinct()
        dbRoots.forEach { dbRoot ->
            val database = dbRoot.resolve("sessions.db")
            if (!Files.isRegularFile(database) || closed.get()) return@forEach
            try {
                LocalUsageFiles.openDatabase(database).use { connection ->
                    connection.createStatement().use { statement ->
                        statement.executeQuery("SELECT session_id, messages_path FROM sessions WHERE messages_path IS NOT NULL").use { rows ->
                            while (!closed.get() && rows.next()) {
                                val id = rows.getString(1)
                                if (id in seen) continue
                                val messages = runCatching { Path.of(rows.getString(2)).toAbsolutePath().normalize() }.getOrNull() ?: continue
                                val allowed = roots.any { messages.startsWith(it) } || messages.startsWith(dbRoot.parent)
                                if (!allowed || !Files.isRegularFile(messages)) { unreadable = true; continue }
                                var usage = ClineUsage()
                                messageMetrics(messages) { metrics ->
                                    usage = usage.copy(inputTokens = usage.inputTokens + metrics.count("inputTokens"),
                                        outputTokens = usage.outputTokens + metrics.count("outputTokens"),
                                        cacheReadTokens = usage.cacheReadTokens + metrics.count("cacheReadTokens"),
                                        cacheWriteTokens = usage.cacheWriteTokens + metrics.count("cacheWriteTokens"),
                                        costUsd = metrics.numberValue("cost")?.let { (usage.costUsd ?: 0.0) + it } ?: usage.costUsd)
                                }
                                add(id, usage)
                            }
                        }
                    }
                }
                sources++
            } catch (_: Exception) { unreadable = true }
        }
        return result.copy(sources = sources, updatedAt = System.currentTimeMillis(), error = when {
            unreadable -> "Some local Cline usage files could not be read; totals may be incomplete."
            sources == 0 -> "No local Cline task usage found. Use Cline in VS Code, Cursor, or the CLI first."
            else -> null
        })
    }

    private fun arrayObjects(path: Path, read: (JsonObject) -> Unit) {
        Files.newBufferedReader(path).use { input ->
            JsonReader(input).use { json ->
                if (json.peek() != JsonToken.BEGIN_ARRAY) error("Unsupported local Cline usage format")
                json.beginArray()
                while (!closed.get() && json.hasNext()) {
                    val entry = JsonParser.parseReader(json)
                    if (entry.isJsonObject) read(entry.asJsonObject)
                }
            }
        }
    }

    /** SDK v0.0.90 wraps messages in a document; skip all content and system-prompt fields. */
    private fun messageMetrics(path: Path, read: (JsonObject) -> Unit) {
        Files.newBufferedReader(path).use { input ->
            JsonReader(input).use { json ->
                fun messages() {
                    check(json.peek() == JsonToken.BEGIN_ARRAY) { "Unsupported Cline messages format" }
                    json.beginArray()
                    while (!closed.get() && json.hasNext()) {
                        if (json.peek() != JsonToken.BEGIN_OBJECT) { json.skipValue(); continue }
                        json.beginObject()
                        while (json.hasNext()) {
                            if (json.nextName() == "metrics" && json.peek() == JsonToken.BEGIN_OBJECT)
                                read(JsonParser.parseReader(json).asJsonObject)
                            else json.skipValue()
                        }
                        json.endObject()
                    }
                    if (!closed.get()) json.endArray()
                }
                when (json.peek()) {
                    JsonToken.BEGIN_ARRAY -> messages()
                    JsonToken.BEGIN_OBJECT -> {
                        var found = false
                        json.beginObject()
                        while (!closed.get() && json.hasNext()) {
                            if (json.nextName() == "messages") { messages(); found = true }
                            else json.skipValue()
                        }
                        check(found) { "Missing Cline messages array" }
                        if (!closed.get()) json.endObject()
                    }
                    else -> error("Unsupported Cline messages format")
                }
            }
        }
    }

    private fun JsonObject.count(key: String): Long = numberValue(key)?.toLong() ?: 0
    override fun close() { closed.set(true) }
}
