enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "proxy-essentials-plugin"

include(
    ":proxy-shared",
    ":proxy-velocity",
    ":proxy-bungeecord"
)
