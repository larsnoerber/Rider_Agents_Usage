
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
              <li>AgentMeter 1.1.0: first stable release prepared for JetBrains Marketplace.</li>
              <li>Monitor OpenAI Codex, JetBrains AI, GitHub Copilot, Claude and Cursor in one usage overview and the status bar.</li>
              <li>View provider-reported quotas, subscription plans, reset countdowns and local usage charts. Click a subscription badge to show or hide details.</li>
              <li>Choose installed providers and refresh intervals in configuration. New installations start with JetBrains AI only.</li>
              <li>Enable optional weekly insights, quota-driven boss battles and Tic-Tac-Toe through the Weekly and Games checkboxes.</li>
              <li>Use existing local provider sign-ins. No telemetry or credential storage in plugin settings.</li>
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
