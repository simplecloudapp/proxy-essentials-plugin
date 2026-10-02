package app.simplecloud.plugin.proxy.shared.config.migration

import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

object LegacyDirectoryMigrator {

    private val logger = LoggerFactory.getLogger(LegacyDirectoryMigrator::class.java)
    private val LEGACY_DIRECTORY_NAMES = listOf("proxy-essentials-velocity", "proxy-essentials-bungeecord")

    fun migrate(dataDirectory: Path) {
        val directory = dataDirectory.toAbsolutePath().normalize()
        val pluginsDirectory = directory.parent ?: return
        val legacyDirectory = LEGACY_DIRECTORY_NAMES
            .map { pluginsDirectory.resolve(it) }
            .find { it != directory && Files.isDirectory(it) } ?: return

        if (Files.isDirectory(directory) && Files.list(directory).use { it.findAny().isPresent }) {
            logger.warn(
                "The directory '${legacyDirectory.fileName}' is not used anymore. " +
                    "Rename it to '${directory.fileName}' in your template to get rid of this warning."
            )
            return
        }

        try {
            legacyDirectory.toFile().copyRecursively(directory.toFile(), overwrite = true)
        } catch (e: Exception) {
            logger.error("Could not take over the configuration of '${legacyDirectory.fileName}', so the default configuration is used", e)
            return
        }

        logger.info(
            "Took over the configuration of '${legacyDirectory.fileName}' into '${directory.fileName}'. " +
                "Rename that directory in your template to keep editing your configuration."
        )
    }
}
