
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
    implementation("org.xerial:sqlite-jdbc:3.51.3.0")
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
              <li>Reuse ACP sign-ins for Copilot quotas, Cursor usage and Cline account credits; show Claude API connection or local token usage.</li>
              <li>Default new Rider installations to JetBrains AI only, with Weekly and Games disabled; preserve saved choices.</li>
              <li>Hide missing agents from Usage and the status bar; show ACP installation information in configuration without install buttons.</li>
              <li>Toggle provider details and charts through the subscription badge, without a separate details link.</li>
              <li>Visualize boss hits with provider and points, an impact flash and shake; preserve damage without replaying refreshes.</li>
              <li>Add Tic-Tac-Toe with saved scores and configurable Games visibility.</li>
              <li>Fix the usage chart initialization crash and reading Cline session history.</li>
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
