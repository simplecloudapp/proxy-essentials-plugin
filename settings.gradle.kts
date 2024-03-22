plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "proxy-plugin"
include("proxy-shared")
include("proxy-velocity")
include("proxy-bungeecord")
