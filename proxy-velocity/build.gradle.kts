import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

dependencies {
    api(project(":proxy-shared"))

    compileOnly(rootProject.libs.adventureApi)

    compileOnly(rootProject.libs.velocityApi)
    annotationProcessor(rootProject.libs.velocityApi)
}

tasks.named("shadowJar", ShadowJar::class) {
    relocate("kotlin.", "app.simplecloud.plugin.libs.kotlin.")
}