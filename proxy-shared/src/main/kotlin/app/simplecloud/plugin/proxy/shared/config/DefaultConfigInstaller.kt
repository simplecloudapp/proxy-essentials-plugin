package app.simplecloud.plugin.proxy.shared.config

import java.net.JarURLConnection
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile

object DefaultConfigInstaller {

    private const val RESOURCE_ROOT = "config"

    fun install(dataDirectory: Path, classLoader: ClassLoader) {
        Files.createDirectories(dataDirectory)

        val resourceUrl = classLoader.getResource(RESOURCE_ROOT)
            ?: return installKnownResources(dataDirectory, classLoader)
        when (resourceUrl.protocol) {
            "file" -> installFromDirectory(Path.of(resourceUrl.toURI()), dataDirectory)
            "jar" -> installFromJar(resourceUrl.openConnection() as JarURLConnection, dataDirectory)
            else -> installKnownResources(dataDirectory, classLoader)
        }
    }

    private fun installFromDirectory(resourceDirectory: Path, dataDirectory: Path) {
        Files.walk(resourceDirectory).use { paths ->
            paths
                .filter { Files.isRegularFile(it) }
                .forEach { source ->
                    val target = dataDirectory.resolve(resourceDirectory.relativize(source).toString())
                    copyIfMissing(source, target)
                }
        }
    }

    private fun installFromJar(connection: JarURLConnection, dataDirectory: Path) {
        val root = connection.entryName.trimEnd('/') + "/"
        val jarFile: JarFile = connection.jarFile

        jarFile.entries().asSequence()
            .filter { !it.isDirectory && it.name.startsWith(root) }
            .forEach { entry ->
                val target = dataDirectory.resolve(entry.name.removePrefix(root))
                jarFile.getInputStream(entry).use { input ->
                    copyIfMissingOrDamaged(input.readAllBytes(), target)
                }
            }
    }

    private fun copyIfMissing(source: Path, target: Path) {
        copyIfMissingOrDamaged(Files.readAllBytes(source), target)
    }

    private fun installKnownResources(dataDirectory: Path, classLoader: ClassLoader) {
        KNOWN_RESOURCES.forEach { resource ->
            val target = dataDirectory.resolve(resource.removePrefix("$RESOURCE_ROOT/"))
            val stream = classLoader.getResourceAsStream(resource) ?: return@forEach
            stream.use { copyIfMissingOrDamaged(it.readAllBytes(), target) }
        }
    }

    private fun copyIfMissingOrDamaged(sourceBytes: ByteArray, target: Path) {
        if (Files.exists(target) && !isDamagedDefaultLayout(sourceBytes, target)) return

        Files.createDirectories(target.parent)
        Files.write(target, sourceBytes)
    }

    private fun isDamagedDefaultLayout(sourceBytes: ByteArray, target: Path): Boolean {
        val normalizedPath = target.toString().replace('\\', '/')
        if (!normalizedPath.endsWith("layout/default.yml") && !normalizedPath.endsWith("layout/maintenance.yml")) {
            return false
        }

        val source = sourceBytes.toString(Charsets.UTF_8)
        val targetContent = Files.readString(target)
        val damagedSource = source.removeFirstLayoutVersionScalar()
        val legacySource = source.replaceFirstLayoutConfigVersionWithVersion()
        return (damagedSource != source && targetContent.normalizeLineEndings() == damagedSource.normalizeLineEndings()) ||
            (legacySource != source && targetContent.normalizeLineEndings() == legacySource.normalizeLineEndings())
    }

    private fun String.removeFirstLayoutVersionScalar(): String {
        val lines = lines().toMutableList()
        val firstContentIndex = lines.indexOfFirst { it.isNotBlank() }
        if (firstContentIndex == -1 || !lines[firstContentIndex].isLayoutFileVersion()) return this

        lines.removeAt(firstContentIndex)
        if (firstContentIndex < lines.size && lines[firstContentIndex].isBlank()) {
            lines.removeAt(firstContentIndex)
        }
        return lines.joinToString("\n")
    }

    private fun String.replaceFirstLayoutConfigVersionWithVersion(): String {
        val lines = lines().toMutableList()
        val firstContentIndex = lines.indexOfFirst { it.isNotBlank() }
        if (firstContentIndex == -1 || !lines[firstContentIndex].isLayoutConfigVersion()) return this

        lines[firstContentIndex] = lines[firstContentIndex].replaceFirst("config-version:", "version:")
        return lines.joinToString("\n")
    }

    private fun String.normalizeLineEndings(): String = replace("\r\n", "\n").trimEnd()

    private fun String.isLayoutFileVersion(): Boolean {
        val trimmed = trim()
        return trimmed == "version: '1'" || trimmed == "version: \"1\"" || trimmed == "version: 1"
    }

    private fun String.isLayoutConfigVersion(): Boolean {
        val trimmed = trim()
        return trimmed == "config-version: '1'" || trimmed == "config-version: \"1\"" || trimmed == "config-version: 1"
    }

    private val KNOWN_RESOURCES = listOf(
        "config/config.yml",
        "config/messages.yml",
        "config/placeholder.yml",
        "config/layout/default.yml",
        "config/layout/maintenance.yml",
        "config/layout/server-icons/simplecloud.png",
        "config/layout/server-icons/simplecloud-gray.png"
    )
}
