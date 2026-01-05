package app.simplecloud.plugin.proxy.shared

import app.simplecloud.api.CloudApi
import app.simplecloud.plugin.proxy.shared.config.YamlConfig
import app.simplecloud.plugin.proxy.shared.config.message.MessageConfig
import app.simplecloud.plugin.proxy.shared.config.placeholder.PlaceHolderConfiguration
import app.simplecloud.plugin.proxy.shared.config.state.JoinStateConfiguration
import app.simplecloud.plugin.proxy.shared.config.tablis.TabListConfiguration
import app.simplecloud.plugin.proxy.shared.handler.CloudControllerHandler
import app.simplecloud.plugin.proxy.shared.handler.JoinStateHandler
import app.simplecloud.plugin.proxy.shared.handler.JoinStateResolver
import app.simplecloud.plugin.proxy.shared.handler.MotdLayoutHandler
import app.simplecloud.plugin.proxy.shared.handler.TabListResolver
import java.io.File

open class ProxyPlugin(
    dirPath: String
) {

    val api = CloudApi.create()
    val config = YamlConfig(dirPath)
    val tabListConfiguration = config.load<TabListConfiguration>("tablist")
    val placeHolderConfiguration = config.load<PlaceHolderConfiguration>("placeholder")
    val messagesConfiguration = config.load<MessageConfig>("messages")
    val joinStateConfiguration = config.load<JoinStateConfiguration>("joinstate")

    val motdLayoutHandler = MotdLayoutHandler(File("$dirPath/layout").toPath(), this)
    val joinStateHandler = JoinStateHandler(this)
    val cloudControllerHandler = CloudControllerHandler(this, joinStateHandler)
    val joinStateResolver = JoinStateResolver(this)
    val tabListResolver = TabListResolver { tabListConfiguration.get() }

}