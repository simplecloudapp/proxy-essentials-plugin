dependencies {
    compileOnly(rootProject.libs.gson)

    compileOnly(rootProject.libs.adventure.minimessage)

    implementation(rootProject.libs.configurate.yaml)
    implementation(rootProject.libs.configurate.kotlin)

    compileOnly(rootProject.libs.simplecloud.controller)

    compileOnly(rootProject.libs.command.cloud.core)
}