package app.simplecloud.plugin.proxy.velocity.event

import app.simplecloud.plugin.proxy.shared.event.TabListPlayerConfiguration
import com.velocitypowered.api.proxy.Player

class TabListConfigurationEvent(
    var tabListConfiguration: TabListPlayerConfiguration,
    val player: Player
) {
}