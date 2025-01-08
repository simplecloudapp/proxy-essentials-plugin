plugins {
    alias(libs.plugins.minotaur)
}

dependencies {
    api(project(":proxy-shared"))

    compileOnly(rootProject.libs.bungeecord)

    implementation(rootProject.libs.adventure.legacy.serializer)
    implementation(rootProject.libs.adventure.minimessage)
    implementation(rootProject.libs.adventure.bungeecord.platform)

    compileOnly(rootProject.libs.simplecloud.event.wrapper.bungeecord)

    implementation(rootProject.libs.simplecloud.plugin.api)

    implementation(rootProject.libs.command.cloud.core)
    implementation(rootProject.libs.command.cloud.bungeecord)
}


tasks.shadowJar {
    relocate("org.incendo", "app.simplecloud.relocate.incendo")
    relocate("org.spongepowered", "app.simplecloud.relocate.spongepowered")
    relocate("net.kyori", "app.simplecloud.relocate.kyori")
    relocate("app.simplecloud.plugin.api", "app.simplecloud.relocate.plugin.api")
}

modrinth {
    token.set(project.findProperty("modrinthToken") as String? ?: System.getenv("MODRINTH_TOKEN"))
    projectId.set("WzfN2mLK")
    versionNumber.set(rootProject.version.toString())
    versionType.set("beta")
    uploadFile.set(tasks.shadowJar)
    gameVersions.addAll(
        "1.20",
        "1.20.1",
        "1.20.2",
        "1.20.3",
        "1.20.4",
        "1.20.5",
        "1.20.6",
        "1.21",
        "1.21.1",
        "1.21.2",
        "1.21.3",
        "1.21.4"
    )
    loaders.add("bungeecord")
    changelog.set("https://docs.simplecloud.app/changelog")
    syncBodyFrom.set(rootProject.file("README.md").readText())
}