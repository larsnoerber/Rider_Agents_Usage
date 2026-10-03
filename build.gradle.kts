
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.20"
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

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

    pluginVerification {
        ides {
            create(IntelliJPlatformType.Rider, "2026.2.3.1")
        }
    }

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
              <li>Add a collapsible weekly recap with a provider party, local quota forecast, and weekly boss battle.</li>
              <li>Preserve weekly boss damage across plugin updates and refresh selected providers when the overview opens.</li>
              <li>Organizes Tool Window settings into agent selection, refresh interval, and action sections.</li>
              <li>Opens the usage overview when a provider status widget is clicked.</li>
              <li>Shows the plugin version and provides a GitHub repository link in configuration.</li>
              <li>Shows four compact facts per agent, with expandable quota, reset, report, plan and status details.</li>
              <li>Improves JetBrains subscription lookup through application services and activation snapshots, and exposes safe lookup diagnostics.</li>
              <li>Corrects Copilot Free quota selection: 100% available means 0% consumed and green.</li>
              <li>Keeps enabled status widgets adjacent in OpenAI, JetBrains AI, Copilot order.</li>
              <li>Resolves active JetBrains AI subscription metadata across separate content-module class loaders.</li>
              <li>Adds agent checkboxes for overview and status bar visibility, and pauses deselected provider reads.</li>
              <li>Separates agents into compact accented cards with subscription badges and visible quota/reset details.</li>
              <li>Colors percentage text directly and removes status dots: OpenAi | D=% - W=%, JetBrainAi | %, Copilot | %.</li>
              <li>Shows GitHub Copilot consumption from 0% unused to 100% exhausted, with matching bars and warnings.</li>
              <li>Reads the active JetBrains AI license name instead of a generic auth tier.</li>
              <li>Adds an illustrated usage preview to the plugin description and README.</li>
              <li>Organizes provider code by feature and separates shared components, settings, and Tool Window composition.</li>
              <li>Shares status widget lifecycle and coordinates all provider refreshes centrally.</li>
              <li>Documents project rules, architecture, and local development.</li>
              <li>Compacts usage rows and headers while keeping colored bars and balances visible.</li>
              <li>Keeps complete reset and sync details in tooltips and labels OpenAI in the status bar.</li>
              <li>Adds an AI chip logo and matching Tool Window icon.</li>
              <li>Monitors OpenAI Codex, JetBrains AI, and GitHub Copilot usage in IntelliJ IDEA and Rider.</li>
              <li>Displays independent status bar widgets with quota percentages and color indicators.</li>
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
    val isWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)

    register<Exec>("buildVsCodeExtension") {
        group = "build"
        description = "Compile and package the Visual Studio Code extension."
        workingDir = layout.projectDirectory.dir("vscode").asFile
        if (isWindows) {
            commandLine("cmd.exe", "/d", "/c", "npm run package")
        } else {
            commandLine("npm", "run", "package")
        }
    }

    register("buildAllExtensions") {
        group = "build"
        description = "Build all editor packages supported on the current operating system."
        dependsOn("buildPlugin", "buildVsCodeExtension")
        if (isWindows) dependsOn("buildVisualStudioExtension")
    }

    register<Exec>("buildVisualStudioExtension") {
        group = "build"
        description = "Build and package the Visual Studio 2022/2026 extension on Windows."
        workingDir = layout.projectDirectory.asFile
        commandLine("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "visualstudio/build.ps1")
        doFirst { check(isWindows) { "Visual Studio packaging requires Windows and Visual Studio MSBuild." } }
    }

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
        compilerOptions.freeCompilerArgs.add("-jvm-default=no-compatibility")
    }
}
