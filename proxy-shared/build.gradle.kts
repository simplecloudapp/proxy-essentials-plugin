dependencies {
    compileOnly(rootProject.libs.adventure.minimessage)

    testImplementation(kotlin("test"))
    testImplementation(rootProject.libs.adventure.minimessage)

    implementation(rootProject.libs.configurate.yaml)
    implementation(rootProject.libs.configurate.kotlin)

    compileOnly(rootProject.libs.command.cloud.core)
}
