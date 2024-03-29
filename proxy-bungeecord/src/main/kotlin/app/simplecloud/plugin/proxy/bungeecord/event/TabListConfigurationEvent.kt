package app.simplecloud.plugin.proxy.bungeecord.event

import app.simplecloud.plugin.proxy.shared.event.TabListPlayerConfiguration
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Event

class TabListConfigurationEvent(
    var tabListConfiguration: TabListPlayerConfiguration,
    var player: ProxiedPlayer
): Event() {
}