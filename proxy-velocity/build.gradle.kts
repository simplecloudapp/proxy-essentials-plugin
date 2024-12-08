import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

dependencies {
    api(project(":proxy-shared"))

    compileOnly(rootProject.libs.adventure.minimessage)

    compileOnly(rootProject.libs.velocity)
    annotationProcessor(rootProject.libs.velocity)

    compileOnly(rootProject.libs.simplecloud.event.wrapper.velocity)
    compileOnly(rootProject.libs.simplecloud.controller)

    implementation(rootProject.libs.command.cloud.core)
    implementation(rootProject.libs.command.cloud.velocity)
}