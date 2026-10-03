package app.simplecloud.plugin.proxy.bungeecord.listener

import app.simplecloud.plugin.proxy.bungeecord.platform.BungeeProxyPlayer
import app.simplecloud.plugin.proxy.shared.ProxyEssentials
import net.md_5.bungee.api.event.ServerConnectedEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler

class TabListListener(
    private val essentials: ProxyEssentials
) : Listener {

    @EventHandler
    fun onServerConnected(event: ServerConnectedEvent) {
        essentials.tabListService.update(BungeeProxyPlayer(event.player))
    }
}
