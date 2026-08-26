import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
}

allprojects {
    group = "app.simplecloud.plugin"
    version = "0.0.13"

    repositories {
        mavenCentral()
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
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://repo.simplecloud.app/snapshots")
    }

    dependencies {
        implementation(rootProject.libs.kotlin.reflect)
        implementation(rootProject.libs.kotlinx.coroutines.core)
        testImplementation(rootProject.libs.kotlin.test)

        compileOnly(rootProject.libs.slf4j.api)
        compileOnly(rootProject.libs.simplecloud.api)

        implementation(rootProject.libs.simplecloud.plugin)
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
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
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
