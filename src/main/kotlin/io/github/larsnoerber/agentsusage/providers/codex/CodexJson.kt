package io.github.larsnoerber.agentsusage.providers.codex

import com.google.gson.JsonObject

internal fun JsonObject.objectValue(name: String): JsonObject? =
    get(name)?.takeIf { it.isJsonObject }?.asJsonObject

internal fun JsonObject.stringValue(name: String): String? =
    get(name)?.takeIf { it.isJsonPrimitive }?.asString
