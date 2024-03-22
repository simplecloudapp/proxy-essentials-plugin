dependencies {
    api(project(":proxy-shared"))

    compileOnly(rootProject.libs.bungeecordApi)

    implementation(rootProject.libs.adventureLegacySerializer)
    implementation(rootProject.libs.adventureApi)
    implementation(rootProject.libs.adventureBungeecordPlatform)
}