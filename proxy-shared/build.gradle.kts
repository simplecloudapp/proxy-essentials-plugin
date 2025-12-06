dependencies {
    compileOnly(rootProject.libs.gson)

    compileOnly(rootProject.libs.adventure.minimessage)

    implementation(rootProject.libs.configurate.yaml)
    implementation(rootProject.libs.configurate.kotlin) {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.jetbrains.kotlinx")
    }

    compileOnly(libs.simplecloud.api)

    compileOnly(rootProject.libs.command.cloud.core)
}