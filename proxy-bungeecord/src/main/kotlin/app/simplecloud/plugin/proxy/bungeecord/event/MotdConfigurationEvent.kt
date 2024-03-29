package app.simplecloud.plugin.proxy.bungeecord.event

import app.simplecloud.plugin.proxy.shared.event.MotdConfiguration
import net.md_5.bungee.api.plugin.Event

class MotdConfigurationEvent(
    var pingConfiguration: MotdConfiguration
): Event() {
}