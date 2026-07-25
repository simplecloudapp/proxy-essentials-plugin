plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include("proxy-bungeecord", "proxy-shared", "proxy-velocity")

rootProject.name = "proxy-essentials-plugin"