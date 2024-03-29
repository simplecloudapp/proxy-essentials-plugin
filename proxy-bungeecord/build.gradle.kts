dependencies {
    api(project(":proxy-shared"))

    compileOnly(rootProject.libs.bungeecordApi)

    implementation(rootProject.libs.adventureLegacySerializer)
    implementation(rootProject.libs.adventureMinimessage)
    implementation(rootProject.libs.adventureBungeecordPlatform)
}