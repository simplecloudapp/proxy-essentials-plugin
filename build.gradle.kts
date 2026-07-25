import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
}

allprojects {
    group = "app.simplecloud.plugin"
    version = "0.0.5"

    repositories {
        mavenCentral()
        maven("https://buf.build/gen/maven")
        maven("https://repo.simplecloud.app/snapshots")
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

subprojects {
    apply {
        plugin("kotlin")
        plugin("com.gradleup.shadow")
    }

    repositories {
        mavenCentral()
        maven("https://buf.build/gen/maven")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.simplecloud.app/snapshots")
    }

    dependencies {
        testImplementation(rootProject.libs.kotlin.test)
        implementation(rootProject.libs.kotlin.reflect)
        implementation(rootProject.libs.kotlinx.coroutines.core)
        implementation(rootProject.libs.simplecloud.plugin)
        compileOnly(rootProject.libs.simplecloud.api)
    }

    kotlin {
        jvmToolchain(25)
        compilerOptions {
            jvmTarget = JvmTarget.JVM_25
            languageVersion = KotlinVersion.KOTLIN_2_4
            apiVersion = KotlinVersion.KOTLIN_2_4
        }
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    }

    tasks.test {
        useJUnitPlatform()
    }

    tasks.shadowJar {
        mergeServiceFiles()
        archiveFileName.set("${project.name}.jar")
    }

    tasks.processResources {
        filesMatching("plugin.yml") {
            expand(
                "version" to project.version
            )
        }
    }

}