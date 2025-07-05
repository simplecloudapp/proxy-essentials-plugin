package app.simplecloud.plugin.proxy.shared.config

import app.simplecloud.plugin.proxy.shared.config.reactive.ReactiveConfig
import app.simplecloud.plugin.proxy.shared.config.reactive.ReactiveConfigInfo
import kotlinx.coroutines.*
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.loader.ParsingException
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.nio.file.*
import java.util.concurrent.ConcurrentHashMap

open class YamlConfig(val dirPath: String) {

    private val watchService = FileSystems.getDefault().newWatchService()
    private val configCache = ConcurrentHashMap<String, Any>()
    private val reactiveConfigs = ConcurrentHashMap<String, MutableList<ReactiveConfigInfo<*>>>()
    private var watcherJob: Job? = null

    init {
        startWatcher()
    }

    inline fun <reified T> load(): ReactiveConfig<T> {
        return load(null)
    }

    inline fun <reified T> load(path: String?): ReactiveConfig<T> {
        return ReactiveConfig(this, path, T::class.java)
    }

    internal fun <T> loadDirect(path: String?, clazz: Class<T>): T? {
        val cacheKey = path ?: "default"

        try {
            val node = buildNode(path).first
            val config = node.get(clazz)

            if (config != null) {
                configCache[cacheKey] = config
            }

            return config
        } catch (ex: ParsingException) {
            val file = File(if (path != null) "${dirPath}/${path.lowercase()}.yml" else dirPath)
            println("Could not load config file ${file.name}. Using cached version if available.")
            @Suppress("UNCHECKED_CAST")
            return configCache[cacheKey] as? T
        }
    }

    internal fun <T> registerReactiveConfig(path: String?, clazz: Class<T>, reactiveConfig: ReactiveConfig<T>) {
        val cacheKey = path ?: "default"
        val configs = reactiveConfigs.getOrPut(cacheKey) { mutableListOf() }
        configs.add(ReactiveConfigInfo(clazz, reactiveConfig))
    }

    fun buildNode(path: String?): Pair<CommentedConfigurationNode, YamlConfigurationLoader> {
        val file = File(if (path != null) "${dirPath}/${path.lowercase()}.yml" else dirPath)
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
        val loader = YamlConfigurationLoader.builder()
            .path(file.toPath())
            .nodeStyle(NodeStyle.BLOCK)
            .defaultOptions { options ->
                options.serializers { builder ->
                    builder.registerAnnotatedObjects(objectMapperFactory())
                }
            }.build()
        return Pair(loader.load(), loader)
    }

    fun <T> save(obj: T) {
        save(null, obj)
    }

    fun <T> save(path: String?, obj: T) {
        val pair = buildNode(path)
        pair.first.set(obj)
        pair.second.save(pair.first)

        // Update cache after successful save
        val cacheKey = path ?: "default"
        if (obj != null) {
            configCache[cacheKey] = obj
        }
    }

    fun <T> save(path: String?, reactiveConfig: ReactiveConfig<T>) {
        return save(path, reactiveConfig.get())
    }

    private fun startWatcher() {
        val directory = File(dirPath).toPath()
        if (!directory.toFile().exists()) {
            directory.toFile().mkdirs()
        }

        try {
            directory.register(
                watchService,
                StandardWatchEventKinds.ENTRY_MODIFY
            )

            watcherJob = CoroutineScope(Dispatchers.IO).launch {
                while (isActive) {
                    try {
                        val key = watchService.take()
                        for (event in key.pollEvents()) {
                            val path = event.context() as? Path ?: continue
                            val resolvedPath = directory.resolve(path)

                            if (Files.isDirectory(resolvedPath) || !resolvedPath.toString().endsWith(".yml")) {
                                continue
                            }

                            val kind = event.kind()
                            if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                                handleFileChange(resolvedPath.toFile())
                            }
                        }
                        key.reset()
                    } catch (e: Exception) {
                        println("Error in config watcher: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            println("Could not start config file watcher: ${e.message}")
        }
    }

    private fun handleFileChange(file: File) {
        val fileName = file.nameWithoutExtension
        val cacheKey = if (file.name == File(dirPath).name) "default" else fileName

        val reactiveConfigList = reactiveConfigs[cacheKey] ?: return

        reactiveConfigList.forEach { configInfo ->
            try {
                val configPath = if (cacheKey == "default") null else fileName

                @Suppress("UNCHECKED_CAST")
                val typedConfigInfo = configInfo as ReactiveConfigInfo<Any>
                val newValue = loadDirect(configPath, typedConfigInfo.clazz)

                typedConfigInfo.reactiveConfig.update(newValue)

            } catch (ex: ParsingException) {
                println("Config file ${file.name} has parsing errors. Keeping old version.")
                // Don't update the reactive config, keep the old cached version
            } catch (e: Exception) {
                println("Error updating reactive config: ${e.message}")
            }
        }
    }

    fun close() {
        watcherJob?.cancel()
        try {
            watchService.close()
        } catch (e: Exception) {
            println("Error closing watch service: ${e.message}")
        }
    }

}
