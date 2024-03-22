package app.simplecloud.plugin.proxy.velocity.event

import app.simplecloud.plugin.proxy.shared.config.motd.MaxPlayerDisplayType
import net.kyori.adventure.text.Component

class ProxyPingConfigurationEvent(
    var messageOfTheDay: Component,
    var playerInfo: List<String>,
    var versionName: String?,
    var maxPlayerDisplayType: MaxPlayerDisplayType?,
    var dynamicPlayerRange: Int
) {
}