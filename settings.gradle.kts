pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "MALLA_MVP"
include(":app")
include(":core")
include(":data")
include(":crypto")
include(":events")
include(":identity")
include(":transport")
include(":media")
include(":network")
