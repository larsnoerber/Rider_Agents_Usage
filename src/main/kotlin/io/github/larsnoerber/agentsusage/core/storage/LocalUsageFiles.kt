package io.github.larsnoerber.agentsusage.core.storage

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.sqlite.JDBC
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.util.Properties

/** Official application stores only; never copy a credential database into our own storage. */
internal object LocalUsageFiles {
    val home: Path get() = Path.of(System.getProperty("user.home"))

    fun configRoot(app: String): Path = when {
        System.getProperty("os.name").startsWith("Windows", true) ->
            absoluteEnvironment("APPDATA")?.resolve(app) ?: home.resolve("AppData/Roaming/$app")
        System.getProperty("os.name").startsWith("Mac", true) -> home.resolve("Library/Application Support/$app")
        else -> (absoluteEnvironment("XDG_CONFIG_HOME") ?: home.resolve(".config")).resolve(app)
    }

    fun absoluteEnvironment(name: String): Path? = System.getenv(name)?.takeIf(String::isNotBlank)
        ?.let { runCatching { Path.of(it).takeIf(Path::isAbsolute) }.getOrNull() }

    fun json(path: Path): JsonObject = Files.newBufferedReader(path).use {
        JsonParser.parseReader(it).asJsonObject
    }

    fun openDatabase(path: Path): Connection {
        require(Files.isRegularFile(path))
        val sidecarsMissing = !Files.exists(Path.of("$path-wal")) && !Files.exists(Path.of("$path-shm"))
        val uri = path.toAbsolutePath().toUri().toASCIIString()
        val connection = JDBC().connect("jdbc:sqlite:$uri?mode=ro${if (sidecarsMissing) "&immutable=1" else ""}", Properties())
        try {
            connection.createStatement().use { statement -> statement.execute("PRAGMA busy_timeout=500") }
            return connection
        } catch (failure: Exception) {
            connection.close()
            throw failure
        }
    }

    fun databaseValue(path: Path, key: String): String? = openDatabase(path).use { connection ->
        connection.prepareStatement("SELECT value FROM ItemTable WHERE key = ? LIMIT 1").use { statement ->
            statement.setString(1, key)
            statement.executeQuery().use { rows ->
                if (!rows.next()) null else {
                    val bytes = rows.getBytes(1) ?: return@use null
                    when {
                        bytes.size > 1 && bytes[0] == 0xff.toByte() && bytes[1] == 0xfe.toByte() ->
                            String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16LE)
                        bytes.size > 1 && bytes[0] == 0xfe.toByte() && bytes[1] == 0xff.toByte() ->
                            String(bytes, 2, bytes.size - 2, StandardCharsets.UTF_16BE)
                        bytes.size > 1 && bytes[1] == 0.toByte() -> String(bytes, StandardCharsets.UTF_16LE)
                        else -> String(bytes, StandardCharsets.UTF_8).removePrefix("\uFEFF")
                    }
                }
            }
        }
    }
}

internal fun JsonObject.objectValue(name: String): JsonObject? = get(name)?.takeIf { it.isJsonObject }?.asJsonObject
internal fun JsonObject.stringValue(name: String): String? = get(name)
    ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString?.takeIf(String::isNotBlank)
internal fun JsonObject.numberValue(name: String): Double? = get(name)
    ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
    ?.let { runCatching { it.asDouble.takeIf { value -> value.isFinite() && value >= 0 } }.getOrNull() }
