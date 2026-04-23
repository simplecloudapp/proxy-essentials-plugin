package app.simplecloud.plugin.proxy.shared.temp

import kotlinx.coroutines.*
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.ConfigurationOptions
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.loader.ParsingException
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.BufferedReader
import java.io.File
import java.io.StringReader
import java.lang.reflect.Type
import java.nio.file.*

abstract class YamlDirectoryRepository<E>(
    private val directory: Path,
    private val clazz: Class<E>,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val watchService = FileSystems.getDefault().newWatchService()
    private var watcherJob: Job? = null
    private val loaders = mutableMapOf<File, YamlConfigurationLoader>()
    protected val entities = mutableMapOf<File, E>()

    fun delete(element: E): Boolean {
        val file = entities.keys.find { entities[it] == element } ?: return false
        return deleteFile(file)
    }

    fun getAll(): List<E> {
        return entities.values.toList()
    }

    fun load(): List<E> {
        if (!directory.toFile().exists()) {
            directory.toFile().mkdirs()
        }

        ensureWatcherRegistered()

        return Files.list(directory)
            .toList()
            .filter { !it.toFile().isDirectory && it.toString().endsWith(".yml") }
            .mapNotNull { load(it.toFile()) }
    }

    open fun watchUpdateEvent(file: File) {}

    protected open fun transformBeforeLoad(file: File, content: String): String = content

    private fun load(file: File): E? {
        try {
            val loader = getOrCreateLoader(file, forRead = true)
            val node = loader.load(ConfigurationOptions.defaults())
            val entity = node.get(clazz) ?: return null
            entities[file] = entity
            return entity
        } catch (ex: ParsingException) {
            val existedBefore = entities.containsKey(file)
            if (existedBefore) {
                return null
            }
            return null
        } catch (ex: SerializationException) {
            val existedBefore = entities.containsKey(file)
            if (existedBefore) {
                return null
            }
            return null
        }
    }

    private fun deleteFile(file: File): Boolean {
        val deletedSuccessfully = file.delete()
        val removedSuccessfully = entities.remove(file) != null
        return deletedSuccessfully && removedSuccessfully
    }

    fun save(fileName: String, entity: E) {
        val file = directory.resolve(fileName).toFile()
        val loader = getOrCreateLoader(file, forRead = false)
        val node = loader.createNode(ConfigurationOptions.defaults().serializers {
            it.register(Enum::class.java, GenericEnumSerializer)
        })
        node.set(clazz, entity)
        loader.save(node)
        entities[file] = entity
    }

    private fun getOrCreateLoader(file: File, forRead: Boolean): YamlConfigurationLoader {
        if (forRead) {
            return createLoader(file, transformBeforeLoad(file, file.readText()))
        }

        return loaders.getOrPut(file) {
            createLoader(file)
        }
    }

    private fun createLoader(file: File, sourceContent: String? = null): YamlConfigurationLoader {
        val builder = YamlConfigurationLoader.builder()
            .path(file.toPath())
            .nodeStyle(NodeStyle.BLOCK)
            .nodeStyle(NodeStyle.BLOCK)
            .defaultOptions { options ->
                options.serializers { builder ->
                    builder.registerAnnotatedObjects(objectMapperFactory())
                    builder.register(Enum::class.java, GenericEnumSerializer)
                }
            }

        if (sourceContent != null) {
            builder.source { BufferedReader(StringReader(sourceContent)) }
        }

        return builder.build()
    }

    private fun ensureWatcherRegistered() {
        if (watcherJob?.isActive == true) {
            return
        }
        directory.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY
        )

        watcherJob = scope.launch {
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
                        when (kind) {
                            StandardWatchEventKinds.ENTRY_CREATE -> {
                                load(resolvedPath.toFile())
                            }

                            StandardWatchEventKinds.ENTRY_MODIFY -> {
                                load(resolvedPath.toFile())
                                watchUpdateEvent(resolvedPath.toFile())
                            }

                            StandardWatchEventKinds.ENTRY_DELETE -> {
                                deleteFile(resolvedPath.toFile())
                            }
                        }
                    }
                    if (!key.reset()) {
                        break
                    }
                } catch (_: ClosedWatchServiceException) {
                    break
                } catch (_: InterruptedException) {
                    break
                }
            }
        }
    }

    fun close() {
        watcherJob?.cancel()
        scope.cancel()
        watchService.close()
    }

    private object GenericEnumSerializer : TypeSerializer<Enum<*>> {
        override fun deserialize(type: Type, node: ConfigurationNode): Enum<*> {
            val value = node.string ?: throw SerializationException("No value present in node")

            if (type !is Class<*> || !type.isEnum) {
                throw SerializationException("Type is not an enum class")
            }

            @Suppress("UNCHECKED_CAST")
            return try {
                java.lang.Enum.valueOf(type as Class<out Enum<*>>, value)
            } catch (e: IllegalArgumentException) {
                throw SerializationException("Invalid enum constant")
            }
        }

        override fun serialize(type: Type, obj: Enum<*>?, node: ConfigurationNode) {
            node.set(obj?.name)
        }
    }

}
