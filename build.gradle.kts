
plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.20"
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = "io.github.larsnoerber.agentsusage"
version = "1.0.2"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("2026.1.2")
    }
}

intellijPlatform {
    instrumentCode = false

    pluginConfiguration {
        ideaVersion {
            sinceBuild = "261"
        }
        vendor {
            name = "Lars Nörber"
            url = "https://github.com/larsnoerber/Rider_Agents_Usage"
        }
        changeNotes = """
            <ul>
              <li>Reads the active JetBrains AI license name instead of a generic auth tier.</li>
              <li>Adds an illustrated usage preview to the plugin description and README.</li>
              <li>Organizes provider code by feature and separates shared components, settings, and Tool Window composition.</li>
              <li>Shares status widget lifecycle and coordinates all provider refreshes centrally.</li>
              <li>Documents project rules, architecture, and local development.</li>
              <li>Compacts usage rows and headers while keeping colored bars and balances visible.</li>
              <li>Moves reset and sync details into tooltips and labels OpenAI in the status bar.</li>
              <li>Adds an AI chip logo and matching Tool Window icon.</li>
              <li>Monitors OpenAI Codex, JetBrains AI, and GitHub Copilot usage in IntelliJ IDEA and Rider.</li>
              <li>Displays independent status bar widgets with remaining usage and color indicators.</li>
              <li>Shows Codex 5-hour and weekly quotas, credits, reset times, and countdowns.</li>
              <li>Shows JetBrains AI subscription credits, top-up credits, and reset times.</li>
              <li>Shows GitHub Copilot premium request or AI credit usage, chat and completion quotas, and reset times.</li>
              <li>Displays subscription plans and compact colored usage bars in a scrollable overview.</li>
              <li>Refreshes all available agents manually or automatically using shared Agent settings.</li>
              <li>Supports refresh presets and custom intervals between 10 and 3600 seconds.</li>
              <li>Discovers the local Codex CLI automatically or uses a configured path.</li>
              <li>Reads usage through the local Codex CLI and installed provider plugins.</li>
              <li>Provides an English interface, settings, tooltips, and error messages.</li>
            </ul>
        """.trimIndent()
    }
}

kotlin {
    jvmToolchain(25)
}

tasks {
    processResources {
        from(listOf("LICENSE", "EULA.md")) {
            into("META-INF")
        }
    }

    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
