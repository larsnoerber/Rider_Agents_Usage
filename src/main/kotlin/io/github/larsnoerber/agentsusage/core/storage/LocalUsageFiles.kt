package io.github.larsnoerber.agentsusage.core.storage

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.file.Files
import java.nio.file.Path

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


}

internal fun JsonObject.objectValue(name: String): JsonObject? = get(name)?.takeIf { it.isJsonObject }?.asJsonObject
internal fun JsonObject.stringValue(name: String): String? = get(name)
    ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString?.takeIf(String::isNotBlank)
internal fun JsonObject.numberValue(name: String): Double? = get(name)
    ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }
    ?.let { runCatching { it.asDouble.takeIf { value -> value.isFinite() && value >= 0 } }.getOrNull() }
