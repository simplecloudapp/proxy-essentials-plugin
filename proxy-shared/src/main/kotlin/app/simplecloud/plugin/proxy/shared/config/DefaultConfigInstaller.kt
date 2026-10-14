package app.simplecloud.plugin.proxy.shared.config

import java.nio.file.Files
import java.nio.file.Path

object DefaultConfigInstaller {

    private val RESOURCES = listOf(
        "config.yml",
        "messages.yml",
        "layout/public.yml",
        "layout/maintenance.yml",
        "layout/server-icons/simplecloud.png",
        "layout/server-icons/simplecloud-gray.png"
    )

    fun install(dataDirectory: Path, classLoader: ClassLoader) {
        RESOURCES.forEach {
            val target = dataDirectory.resolve(it)
            val resourceName = "config/$it"

            if (Files.exists(target)) return@forEach

            val resource = checkNotNull(classLoader.getResourceAsStream(resourceName)) {
                "Missing bundled $resourceName"
            }

            Files.createDirectories(target.parent)
            resource.use { stream -> Files.copy(stream, target) }
        }
    }
}