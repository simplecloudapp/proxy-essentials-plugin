dependencies {
    api(project(":proxy-api"))
    api(project(":proxy-shared"))

    compileOnly(rootProject.libs.velocityApi)
    annotationProcessor(rootProject.libs.velocityApi)
}