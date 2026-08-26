package app.simplecloud.plugin.proxy.shared.motd

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

class MotdLayoutRepository(
    private val directory: Path,
    private val plugin: ProxyPlugin
) : YamlDirectoryRepository<MotdLayoutConfiguration, String>(
    directory,
    MotdLayoutConfiguration::class.java,
) {

    private val logger = Logger.getLogger(MotdLayoutRepository::class.java.name)
    private val layoutsByName = ConcurrentHashMap<String, MotdLayoutConfiguration>()
    private val queueIndexes = ConcurrentHashMap<String, AtomicInteger>()
    private val listenerRegistered = AtomicBoolean(false)

    @Volatile
    private var localLayout: String? = null

    override fun save(entity: MotdLayoutConfiguration) {
        val name = entity.motd.layouts.firstOrNull()?.name ?: plugin.config.get().initialLayout
        save("$name$SUFFIX", entity)
        layoutsByName[name] = entity
    }

    override fun find(identifier: String): MotdLayoutConfiguration? = layoutsByName[identifier]

    fun getLayoutByName(name: String): MotdLayoutConfiguration? = find(name)

    fun loadMotdLayouts() {
        if (!Files.isDirectory(directory)) {
            layoutsByName.clear()
            return
        }

        val loaded = mutableMapOf<String, MotdLayoutConfiguration>()
        getLayoutFileNames().forEach { name ->
            val layout = loadLayout(name)
            if (layout != null) {
                loaded[name] = layout
            }
        }

        layoutsByName.keys.retainAll(loaded.keys)
        layoutsByName.putAll(loaded)
    }

    fun getAvailableLayouts(): List<String> = getLayoutFileNames().sorted()

    fun getCurrentMotdLayout(): MotdLayoutConfiguration {
        val currentLayout = localLayout
        val layoutName = when {
            currentLayout != null && layoutsByName.containsKey(currentLayout) -> currentLayout
            else -> getFallbackLayoutName()
        }
        return find(layoutName) ?: MotdLayoutConfiguration()
    }

    fun setLocalLayout(layoutName: String?, logChange: Boolean = false) {
        val nextLayout = when {
            layoutName.isNullOrBlank() -> null
            else -> layoutName
        }
        if (localLayout == nextLayout) return

        localLayout = nextLayout
        if (logChange) {
            logger.info("MOTD layout changed to ${nextLayout ?: getFallbackLayoutName()}")
        }
    }

    fun registerListener() {
        if (!listenerRegistered.compareAndSet(false, true)) return

        plugin.api.event().server().onUpdated { event ->
            val currentServer = plugin.cloudControllerHandler.currentServer ?: return@onUpdated
            if (event.serverId != currentServer.serverId) return@onUpdated

            setLocalLayout(event.server.properties?.get(KEY)?.toString(), logChange = true)
        }
    }

    fun selectEntry(layout: MotdLayoutConfiguration, layoutKey: String): MotdEntry? {
        val entries = layout.motd.layouts
        if (entries.isEmpty()) return null

        return when (layout.motd.updateType) {
            MotdUpdateType.RANDOM -> entries.random()
            MotdUpdateType.QUEUE -> {
                val counter = queueIndexes.getOrPut(layoutKey) { AtomicInteger(0) }
                entries[counter.getAndIncrement() % entries.size]
            }
        }
    }

    private fun getLayoutFileNames(): List<String> {
        if (!Files.isDirectory(directory)) return emptyList()

        return Files.list(directory).use { paths ->
            paths
                .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(SUFFIX) }
                .map { it.fileName.toString().removeSuffix(SUFFIX) }
                .toList()
        }
    }

    private fun loadLayout(name: String): MotdLayoutConfiguration? {
        return try {
            load(directory.resolve("$name$SUFFIX").toFile())
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Could not load MOTD layout $name$SUFFIX", e)
            null
        }
    }

    private fun getFallbackLayoutName(): String {
        val config = plugin.config.get()
        val joinState = config.joinstates.find { it.name == plugin.joinStateService.localState }
        return joinState?.forcedMotdLayout ?: config.initialLayout
    }

    companion object {
        const val KEY = "motd-layout"
        private const val SUFFIX = ".yml"
    }
}
