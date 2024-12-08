package app.simplecloud.plugin.proxy.shared

import app.simplecloud.plugin.proxy.shared.config.YamlConfig
import app.simplecloud.plugin.proxy.shared.config.message.MessageConfig
import app.simplecloud.plugin.proxy.shared.config.placeholder.PlaceHolderConfiguration
import app.simplecloud.plugin.proxy.shared.config.tablis.TabListConfiguration
import app.simplecloud.plugin.proxy.shared.handler.CloudControllerHandler
import app.simplecloud.plugin.proxy.shared.handler.MotdLayoutHandler

open class ProxyPlugin(
    dirPath: String
) {

    val config = YamlConfig(dirPath)
    val tabListConfiguration = config.load<TabListConfiguration>("tablist")!!
    val placeHolderConfiguration = config.load<PlaceHolderConfiguration>("placeholder")!!
    val messagesConfiguration = config.load<MessageConfig>("messages")!!
    val cloudControllerHandler = CloudControllerHandler()
    val motdLayoutHandler = MotdLayoutHandler(config, this)

    var maintenance = true

    companion object {
        val JOIN_MAINTENANCE_PERMISSION = "simplecloud.proxy.join.maintenance"
        val JOIN_FULL_PERMISSION = "simplecloud.proxy.join.full"
    }
}