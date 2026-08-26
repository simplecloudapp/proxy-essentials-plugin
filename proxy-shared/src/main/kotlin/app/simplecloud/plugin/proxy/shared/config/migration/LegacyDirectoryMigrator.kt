package app.simplecloud.plugin.proxy.shared.config.migration

import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object LegacyDirectoryMigrator {

    private val logger = LoggerFactory.getLogger(LegacyDirectoryMigrator::class.java)
    private val LEGACY_DIRECTORY_NAMES = listOf("proxy-essentials-velocity", "proxy-essentials-bungeecord")
    private const val MARKER_FILE = ".migrated-from-legacy-directory"

    fun migrate(directory: Path) {
        val legacyDirectory = findLegacyDirectory(directory) ?: return

        when {
            isAlreadyMigrated(directory) -> warnAboutUnusedDirectory(legacyDirectory, directory)
            else -> takeOverConfiguration(legacyDirectory, directory)
        }
    }

    private fun findLegacyDirectory(dataDirectory: Path): Path? {
        val directory = dataDirectory.toAbsolutePath().normalize()
        val pluginsDirectory = directory.parent ?: return null

        return LEGACY_DIRECTORY_NAMES
            .map { pluginsDirectory.resolve(it) }
            .find { it != directory && Files.isDirectory(it) }
    }

    private fun isAlreadyMigrated(directory: Path): Boolean {
        if (Files.exists(directory.resolve(MARKER_FILE))) {
            return true
        }

        if (!Files.isDirectory(directory)) {
            return false
        }

        return Files.list(directory).use { paths -> paths.findAny().isPresent }
    }

    private fun takeOverConfiguration(legacyDirectory: Path, dataDirectory: Path) {
        try {
            copyDirectory(legacyDirectory, dataDirectory)
            Files.createFile(dataDirectory.resolve(MARKER_FILE))
        } catch (e: Exception) {
            logger.error("Could not take over the configuration of '${legacyDirectory.fileName}'")
            return
        }

        logger.info(
            "Took over the configuration of '${legacyDirectory.fileName}' into '${dataDirectory.fileName}'. " +
            "Rename that directory in your template to keep editing your configuration."
        )
    }

    private fun warnAboutUnusedDirectory(legacyDirectory: Path, dataDirectory: Path) {
        logger.warn(
            "The directory '${legacyDirectory.fileName}' is not used anymore. " +
            "Rename it to '${dataDirectory.fileName}' in your template to get rid of this warning."
        )
    }

    private fun copyDirectory(source: Path, target: Path) {
        Files.createDirectories(target)

        Files.walk(source).use { paths ->
            paths.forEach { path -> copyEntry(source, target, path) }
        }
    }

    private fun copyEntry(source: Path, target: Path, path: Path) {
        val destination = target.resolve(source.relativize(path).toString())

        when {
            Files.isDirectory(path) -> Files.createDirectories(destination)
            else -> {
                Files.createDirectories(destination.parent)
                Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
}
