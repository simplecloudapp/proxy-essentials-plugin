import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
}

val baseVersion = "0.0.1"
val commitHash = System.getenv("COMMIT_HASH")
val snapshotversion = "${baseVersion}-dev.$commitHash"

allprojects {
    group = "app.simplecloud.plugin.proxy"
    version = if (commitHash != null) snapshotversion else baseVersion

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "com.gradleup.shadow")

    repositories {
        mavenCentral()
        maven("https://buf.build/gen/maven")

        maven {
            name = "papermc"
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots")
        }
        maven {
            name = "simplecloudRepositorySnapshots"
            url = uri("https://repo.simplecloud.app/snapshots")
        }
    }

    dependencies {
        testImplementation(rootProject.libs.kotlin.test)
        compileOnly(rootProject.libs.kotlin.jvm)
        compileOnly(rootProject.libs.kotlin.coroutines)
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    kotlin {
        jvmToolchain(21)
        compilerOptions {
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        }
    }

    tasks.named("shadowJar", ShadowJar::class) {
        mergeServiceFiles()
        archiveFileName.set("${project.name}.jar")

        relocate("com.google.protobuf", "app.simplecloud.relocate.google.protobuf")
        relocate("com.google.common", "app.simplecloud.relocate.google.common")
        relocate("io.grpc", "app.simplecloud.relocate.io.grpc")
    }

    tasks.test {
        useJUnitPlatform()
    }
}