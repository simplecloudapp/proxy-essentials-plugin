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
