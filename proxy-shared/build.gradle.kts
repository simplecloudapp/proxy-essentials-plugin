dependencies {
    compileOnly(rootProject.libs.gson)

    compileOnly(rootProject.libs.adventure.minimessage)

    implementation(rootProject.libs.configurate.yaml)
    implementation(rootProject.libs.configurate.kotlin) {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.jetbrains.kotlinx")
    }

    compileOnly(rootProject.libs.simplecloud.controller)

    api(rootProject.libs.simplecloud.plugin.api)

    compileOnly(rootProject.libs.command.cloud.core)
}