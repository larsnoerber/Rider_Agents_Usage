pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/ij/intellij-dependencies")
    }
}

rootProject.name = providers.gradleProperty("projectName").get()
