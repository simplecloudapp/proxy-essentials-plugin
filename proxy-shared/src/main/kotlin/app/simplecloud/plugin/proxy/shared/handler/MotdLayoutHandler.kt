package app.simplecloud.plugin.proxy.shared.handler

import app.simplecloud.plugin.api.shared.config.YamlDirectoryRepository
import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import app.simplecloud.plugin.proxy.shared.config.MotdEntry
import app.simplecloud.plugin.proxy.shared.config.MotdLayoutConfiguration
import app.simplecloud.plugin.proxy.shared.config.MotdUpdateType
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import java.util.logging.Logger

class MotdLayoutHandler(
    private val directory: Path,
    private val proxyPlugin: ProxyPlugin
) : YamlDirectoryRepository<MotdLayoutConfiguration, String>(
    directory,
    MotdLayoutConfiguration::class.java,
) {
    private val logger = Logger.getLogger(MotdLayoutHandler::class.java.name)
    private val layoutsByName = ConcurrentHashMap<String, MotdLayoutConfiguration>()
    private val queueIndexes = ConcurrentHashMap<String, AtomicInteger>()
    private val listenerRegistered = AtomicBoolean(false)

    @Volatile
    private var localLayout: String? = null

    companion object {
        const val MOTD_LAYOUT_KEY = "motd-layout"
        private const val LAYOUT_FILE_SUFFIX = ".yml"
    }

    override fun save(entity: MotdLayoutConfiguration) {
        val name = entity.motd.layouts.firstOrNull()?.name ?: proxyPlugin.proxyEssentialsConfig.get().initialLayout
        save("$name$LAYOUT_FILE_SUFFIX", entity)
        layoutsByName[name] = entity
    }

    override fun find(identifier: String): MotdLayoutConfiguration? = layoutsByName[identifier]

    fun loadMotdLayouts() {
        if (!Files.isDirectory(directory)) {
            layoutsByName.clear()
            return
        }

        val loaded = mutableMapOf<String, MotdLayoutConfiguration>()
        Files.list(directory).use { paths ->
            paths
                .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(LAYOUT_FILE_SUFFIX) }
                .forEach { path ->
                    val layout = loadLayout(path) ?: return@forEach
                    loaded[path.fileName.toString().removeSuffix(LAYOUT_FILE_SUFFIX)] = layout
                }
        }

        layoutsByName.keys.retainAll(loaded.keys)
        layoutsByName.putAll(loaded)
    }

    fun getLayoutByName(name: String): MotdLayoutConfiguration? = find(name)

    fun getCurrentMotdLayout(): MotdLayoutConfiguration {
        val layoutName = localLayout?.takeIf(::hasLayout) ?: fallbackLayoutName()
        return find(layoutName) ?: MotdLayoutConfiguration()
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
                .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(LAYOUT_FILE_SUFFIX) }
                .map { it.fileName.toString().removeSuffix(LAYOUT_FILE_SUFFIX) }
                .sorted()
                .toList()
        }
    }

    private fun loadLayout(path: Path): MotdLayoutConfiguration? {
        return try {
            load(path.toFile())
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Could not load MOTD layout ${path.fileName}", e)
            null
        }
    }

    private fun fallbackLayoutName(): String {
        val localState = proxyPlugin.joinStateHandler.localState
        val config = proxyPlugin.proxyEssentialsConfig.get()
        val joinState = config.joinstates.find { it.name == localState }
        return joinState?.forcedMotdLayout ?: config.initialLayout
    }

    private fun hasLayout(layoutName: String): Boolean = layoutsByName.containsKey(layoutName)
}
