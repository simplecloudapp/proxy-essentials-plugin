package app.simplecloud.plugin.proxy.shared.event

import app.simplecloud.plugin.proxy.shared.config.motd.MaxPlayerDisplayType
import net.kyori.adventure.text.Component

data class MotdConfiguration(
    var messageOfTheDay: Component,
    var playerInfo: List<String>,
    var versionName: String,
    var maxPlayerDisplayType: MaxPlayerDisplayType?,
    var dynamicPlayerRange: Int
) {
}
