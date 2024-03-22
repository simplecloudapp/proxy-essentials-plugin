import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

dependencies {
    api(project(":proxy-api"))
    api(project(":proxy-shared"))

    compileOnly(rootProject.libs.adventureApi)

    compileOnly(rootProject.libs.velocityApi)
    annotationProcessor(rootProject.libs.velocityApi)
}