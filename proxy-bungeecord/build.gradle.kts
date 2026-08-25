plugins {
    alias(libs.plugins.minotaur)
}

dependencies {
    api(project(":proxy-shared"))

    compileOnly(rootProject.libs.bungeecord)

    testImplementation(kotlin("test"))
    testImplementation(rootProject.libs.bungeecord)

    implementation(rootProject.libs.adventure.legacy.serializer)
    implementation(rootProject.libs.adventure.minimessage)
    implementation(rootProject.libs.adventure.bungeecord.platform)

    implementation(rootProject.libs.command.cloud.core)
    implementation(rootProject.libs.command.cloud.bungeecord)
}


tasks.shadowJar {
    relocate("net.kyori", "app.simplecloud.relocate.kyori")
}

modrinth {
    token.set(project.findProperty("modrinthToken") as String? ?: System.getenv("MODRINTH_TOKEN"))
    projectId.set("WzfN2mLK")
    versionNumber.set(rootProject.version.toString())
    versionType.set("release")
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
        "1.21.4",
        "1.21.5",
        "1.21.6",
        "1.21.7",
        "1.21.8",
        "1.21.9",
        "1.21.10",
        "1.21.11",
        "26.1",
        "26.1.1",
        "26.1.2",
        "26.2",
    )
    loaders.addAll("bungeecord", "waterfall")
    changelog.set("https://docs.simplecloud.app/changelog")
    syncBodyFrom.set(rootProject.file("README.md").readText())
}
