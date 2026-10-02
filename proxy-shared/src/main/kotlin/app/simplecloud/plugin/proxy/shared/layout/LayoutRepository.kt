package app.simplecloud.plugin.proxy.shared.layout

import app.simplecloud.plugin.api.shared.config.YamlDirectoryRepository
import app.simplecloud.plugin.proxy.shared.config.LayoutConfig
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.*

class LayoutRepository(
    private val directory: Path
) : YamlDirectoryRepository<LayoutConfig, String>(directory, LayoutConfig::class.java) {

    private val logger = LoggerFactory.getLogger(LayoutRepository::class.java)
    private val layouts = AtomicReference<Map<String, LayoutConfig>>(emptyMap())

    override fun find(identifier: String): LayoutConfig? = layouts.get()[identifier]

    override fun save(entity: LayoutConfig) {
        throw UnsupportedOperationException("Layouts are only edited in their files")
    }

    fun getNames(): List<String> = layouts.get().keys.sorted()

    fun loadLayouts() {
        if (!Files.isDirectory(directory)) {
            logger.warn("The layout directory '$directory' does not exist, so the server list uses the default layout.")
            layouts.set(emptyMap())
            return
        }

        val files = Files.list(directory).use { paths -> paths.filter { it.isRegularFile() && it.extension == "yml" }.toList() }
        layouts.set(files.mapNotNull { loadLayout(it) }.toMap())
    }

    private fun loadLayout(file: Path): Pair<String, LayoutConfig>? {
        return try {
            val layout = load(file.toFile()) ?: error("the file is empty")
            file.nameWithoutExtension to layout
        } catch (e: Exception) {
            logger.error("Could not load the layout '${file.fileName}'", e)
            null
        }
    }
}
