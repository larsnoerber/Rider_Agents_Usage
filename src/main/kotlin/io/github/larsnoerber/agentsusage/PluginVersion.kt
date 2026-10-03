package io.github.larsnoerber.agentsusage

import java.util.jar.Manifest

/** Reads the version Gradle writes into the packaged plugin JAR manifest. */
object PluginVersion {
    val current: String by lazy {
        PluginVersion::class.java.getResourceAsStream("/META-INF/MANIFEST.MF")?.use { stream ->
            Manifest(stream).mainAttributes.getValue("Version")
        } ?: "Unknown"
    }
}
