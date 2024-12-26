dependencies {
    api(project(":proxy-shared"))

    compileOnly(rootProject.libs.bungeecord)

    implementation(rootProject.libs.adventure.legacy.serializer)
    implementation(rootProject.libs.adventure.minimessage)
    implementation(rootProject.libs.adventure.bungeecord.platform)

    compileOnly(rootProject.libs.simplecloud.event.wrapper.bungeecord)

    implementation(rootProject.libs.command.cloud.core)
    implementation(rootProject.libs.command.cloud.bungeecord)
}


tasks.shadowJar {
    relocate("org.incendo", "app.simplecloud.relocate.incendo")
    relocate("org.spongepowered", "app.simplecloud.relocate.spongepowered")
    relocate("net.kyori", "app.simplecloud.relocate.kyori")
}