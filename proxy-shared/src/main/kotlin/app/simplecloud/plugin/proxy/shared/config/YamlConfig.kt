package app.simplecloud.plugin.proxy.shared.config

import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.kotlin.objectMapperFactory
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File

open class YamlConfig(private val dirPath: String) {

    inline fun <reified T> load(): T? {
        return load(null)
    }

    fun buildNode(path: String?): Pair<CommentedConfigurationNode, YamlConfigurationLoader> {
        val file = File(if(path != null)"${dirPath}/${path.lowercase()}.yml" else dirPath)
        if(!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
        val loader = YamlConfigurationLoader.builder()
            .path(file.toPath())
            .defaultOptions { options ->
                options.serializers{ builder ->
                    builder.registerAnnotatedObjects(objectMapperFactory())
                }
            }.build()
        return Pair(loader.load(), loader)
    }

    inline fun <reified T> load(path: String?) : T? {
        val node = buildNode(path).first
        return node.get<T>()
    }

    fun <T> save(obj: T) {
        save(null, obj)
    }

    fun <T> save(path: String?, obj: T) {
        val pair = buildNode(path)
        pair.first.set(obj)
        pair.second.save(pair.first)
    }
}