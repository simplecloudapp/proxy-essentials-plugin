import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    base

    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow) apply false
}

val baseVersion = "0.0.1"
val commitHash = System.getenv("COMMIT_HASH")
val snapshotVersion = "${baseVersion}-dev.$commitHash"

allprojects {
    group = "app.simplecloud.plugin.proxy"
    version = if (commitHash != null) snapshotVersion else baseVersion

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = rootProject.libs.plugins.kotlin.get().pluginId)
    apply(plugin = rootProject.libs.plugins.shadow.get().pluginId)

    repositories {
        maven("https://buf.build/gen/maven")

        maven {
            name = "simpleCloudRepositorySnapshots"
            url = uri("https://repo.simplecloud.app/snapshots")
        }
    }

    dependencies {
        implementation(rootProject.libs.kotlin.coroutines)
    }

    tasks {
        withType<ShadowJar>().configureEach {
            mergeServiceFiles()
            archiveFileName.set("${project.name}.jar")
        }
    }

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(21)
        compilerOptions {
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        }
    }
}
