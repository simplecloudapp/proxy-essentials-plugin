import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.minotaur)
}

dependencies {
    api(project(":proxy-shared"))

    compileOnly(rootProject.libs.adventure.minimessage)

    compileOnly(rootProject.libs.velocity)
    annotationProcessor(rootProject.libs.velocity)

    compileOnly(rootProject.libs.simplecloud.controller)

    implementation(rootProject.libs.simplecloud.plugin.api)

    implementation(rootProject.libs.command.cloud.core)
    implementation(rootProject.libs.command.cloud.velocity)
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
        "1.21.4",
        "1.21.5",
        "1.21.6",
        "1.21.7",
    )
    loaders.add("velocity")
    changelog.set("https://docs.simplecloud.app/changelog")
    syncBodyFrom.set(rootProject.file("README.md").readText())
}