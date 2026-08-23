package app.simplecloud.plugin.proxy.shared.handler

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import app.simplecloud.plugin.proxy.shared.config.motd.MotdEntry
import app.simplecloud.plugin.proxy.shared.config.motd.MotdLayoutConfiguration
import app.simplecloud.plugin.proxy.shared.config.motd.MotdUpdateType
import app.simplecloud.plugin.proxy.shared.temp.YamlDirectoryRepository
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger

class MotdLayoutHandler(
    private val directory: Path,
    private val proxyPlugin: ProxyPlugin
) : YamlDirectoryRepository<MotdLayoutConfiguration>(
    directory,
    MotdLayoutConfiguration::class.java,
) {
    private val logger = Logger.getLogger(MotdLayoutHandler::class.java.name)
    private val queueIndexes = ConcurrentHashMap<String, AtomicInteger>()
    private val listenerRegistered = AtomicBoolean(false)

    @Volatile
    private var localLayout: String? = null

    companion object {
        const val MOTD_LAYOUT_KEY = "motd-layout"
    }

    override fun transformBeforeLoad(file: File, content: String): String {
        val lines = content.lines().toMutableList()
        val firstContentIndex = lines.indexOfFirst { it.isNotBlank() }
        if (firstContentIndex == -1 || !lines[firstContentIndex].isLayoutFileVersion()) return content

        val hasVersionSettings = lines
            .drop(firstContentIndex + 1)
            .any { it == "version:" }

        if (!hasVersionSettings) return content

        lines.removeAt(firstContentIndex)
        if (firstContentIndex < lines.size && lines[firstContentIndex].isBlank()) {
            lines.removeAt(firstContentIndex)
        }
        return lines.joinToString(System.lineSeparator())
    }

    fun loadMotdLayouts() {
        load()
    }

    private fun getMotdLayout(name: String): MotdLayoutConfiguration =
        entities[directory.resolve("${name}.yml").toFile()] ?: MotdLayoutConfiguration()

    fun getLayoutByName(name: String): MotdLayoutConfiguration? =
        entities[directory.resolve("${name}.yml").toFile()]

    fun getCurrentMotdLayout(): MotdLayoutConfiguration {
        val layoutName = localLayout?.takeIf(::hasLayout) ?: fallbackLayoutName()
        return getMotdLayout(layoutName)
    }

    fun setLocalLayout(layoutName: String?, logChange: Boolean = false) {
        val nextLayout = layoutName?.takeIf { it.isNotBlank() }
        if (localLayout == nextLayout) return

        localLayout = nextLayout
        if (logChange) {
            logger.info("MOTD layout changed to ${nextLayout ?: fallbackLayoutName()}")
        }
    }

    fun registerListener() {
        if (!listenerRegistered.compareAndSet(false, true)) return

        proxyPlugin.api.event().server().onUpdated { event ->
            val currentServer = proxyPlugin.cloudControllerHandler.currentServer ?: return@onUpdated
            if (event.serverId != currentServer.serverId) return@onUpdated

            val layout = event.server.properties?.get(MOTD_LAYOUT_KEY)?.toString()
            setLocalLayout(layout, logChange = true)
        }
    }

    private fun fallbackLayoutName(): String {
        val localState = proxyPlugin.joinStateHandler.localState
        val config = proxyPlugin.proxyEssentialsConfig.get()
        val joinState = config.joinstates.find { it.name == localState }
        return joinState?.forcedMotdLayout ?: config.initialLayout
    }

    fun selectEntry(layout: MotdLayoutConfiguration, layoutKey: String): MotdEntry? {
        val entries = layout.motd.layouts
        if (entries.isEmpty()) return null
        return when (layout.motd.updateType) {
            MotdUpdateType.RANDOM -> entries.random()
            MotdUpdateType.QUEUE -> {
                val counter = queueIndexes.getOrPut(layoutKey) { AtomicInteger(0) }
                val idx = counter.getAndIncrement() % entries.size
                entries[idx]
            }
        }
    }

    fun getAvailableLayouts(): List<String> {
        if (!Files.isDirectory(directory)) return emptyList()

        return Files.list(directory).use { paths ->
            paths
                .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".yml") }
                .map { it.fileName.toString().removeSuffix(".yml") }
                .sorted()
                .toList()
        }
    }

    private fun hasLayout(layoutName: String): Boolean {
        return Files.isRegularFile(directory.resolve("${layoutName}.yml"))
    }

    private fun String.isLayoutFileVersion(): Boolean {
        val trimmed = trim()
        return trimmed == "version: '1'" || trimmed == "version: \"1\"" || trimmed == "version: 1"
    }
}
