dependencies {
    api(project(":proxy-shared"))

    compileOnly(rootProject.libs.bungeecord)

    implementation(rootProject.libs.adventure.legacy.serializer)
    implementation(rootProject.libs.adventure.minimessage)
    implementation(rootProject.libs.adventure.bungeecord.platform)

    compileOnly(rootProject.libs.simplecloud.event.wrapper.bungeecord)
}