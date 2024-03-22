dependencies {
    api(project(":proxy-shared"))

    compileOnly(rootProject.libs.adventureApi)

    compileOnly(rootProject.libs.velocityApi)
    annotationProcessor(rootProject.libs.velocityApi)
}