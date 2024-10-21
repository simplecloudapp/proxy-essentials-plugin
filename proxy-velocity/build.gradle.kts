import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

dependencies {
    api(project(":proxy-shared"))

    compileOnly(rootProject.libs.adventure.minimessage)

    compileOnly(rootProject.libs.velocity)
    annotationProcessor(rootProject.libs.velocity)
}

tasks.named("shadowJar", ShadowJar::class) {
    //relocate("kotlin.", "app.simplecloud.plugin.libs.kotlin.")
}