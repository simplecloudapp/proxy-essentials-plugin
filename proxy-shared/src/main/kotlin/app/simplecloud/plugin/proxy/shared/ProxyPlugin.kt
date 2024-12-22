package app.simplecloud.plugin.proxy.shared

import app.simplecloud.plugin.proxy.shared.config.YamlConfig
import app.simplecloud.plugin.proxy.shared.config.message.MessageConfig
import app.simplecloud.plugin.proxy.shared.config.placeholder.PlaceHolderConfiguration
import app.simplecloud.plugin.proxy.shared.config.state.JoinStateConfiguration
import app.simplecloud.plugin.proxy.shared.config.tablis.TabListConfiguration
import app.simplecloud.plugin.proxy.shared.handler.CloudControllerHandler
import app.simplecloud.plugin.proxy.shared.handler.JoinStateHandler
import app.simplecloud.plugin.proxy.shared.handler.MotdLayoutHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

open class ProxyPlugin(
    dirPath: String
) {

    val config = YamlConfig(dirPath)
    val tabListConfiguration = config.load<TabListConfiguration>("tablist")!!
    val placeHolderConfiguration = config.load<PlaceHolderConfiguration>("placeholder")!!
    val messagesConfiguration = config.load<MessageConfig>("messages")!!
    val joinStateConfiguration = config.load<JoinStateConfiguration>("joinstate")!!

    val motdLayoutHandler = MotdLayoutHandler(config, this)
    val joinStateHandler = JoinStateHandler(this)
    val cloudControllerHandler = CloudControllerHandler(joinStateHandler)

}