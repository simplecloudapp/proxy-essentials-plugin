dependencies {
    compileOnly(rootProject.libs.adventure.minimessage)

    testImplementation(kotlin("test"))

    implementation(rootProject.libs.configurate.yaml)
    implementation(rootProject.libs.configurate.kotlin) {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.jetbrains.kotlinx")
    }

    compileOnly(rootProject.libs.command.cloud.core)
}
