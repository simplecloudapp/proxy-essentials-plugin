package app.simplecloud.plugin.proxy.shared

import app.simplecloud.plugin.proxy.shared.config.YamlConfig
import app.simplecloud.plugin.proxy.shared.config.message.MessageConfig
import app.simplecloud.plugin.proxy.shared.config.placeholder.PlaceHolderConfiguration
import app.simplecloud.plugin.proxy.shared.config.state.JoinStateConfiguration
import app.simplecloud.plugin.proxy.shared.config.tablis.TabListConfiguration
import app.simplecloud.plugin.proxy.shared.handler.CloudControllerHandler
import app.simplecloud.plugin.proxy.shared.handler.JoinStateHandler
import app.simplecloud.plugin.proxy.shared.handler.MotdLayoutHandler

open class ProxyPlugin(
    dirPath: String
) {

    val config = YamlConfig(dirPath)

    val tabListConfiguration = loadOrCreate("tablist", TabListConfiguration())
    val placeHolderConfiguration = loadOrCreate("placeholder", PlaceHolderConfiguration())
    val messagesConfiguration = loadOrCreate("messages", MessageConfig())
    val joinStateConfiguration = loadOrCreate("joinstate", JoinStateConfiguration())

    val motdLayoutHandler = MotdLayoutHandler(config, this)
    val joinStateHandler = JoinStateHandler(this)
    val cloudControllerHandler = CloudControllerHandler(joinStateHandler)

    private inline fun <reified T : Any> loadOrCreate(path: String, defaultValue: T): T {
        val loaded = config.load<T>(path)
        if (loaded != null) {
            return loaded
        }

        config.save(path, defaultValue)
        return defaultValue
    }

}