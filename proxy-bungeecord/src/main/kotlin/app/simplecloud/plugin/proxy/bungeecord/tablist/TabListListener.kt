package app.simplecloud.plugin.proxy.bungeecord.tablist

import app.simplecloud.plugin.proxy.bungeecord.ProxyBungeeCordPlugin
import net.md_5.bungee.api.event.ServerConnectedEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler

class TabListListener(
    private val plugin: ProxyBungeeCordPlugin
) : Listener {

    @EventHandler
    fun onServerConnected(event: ServerConnectedEvent) {
        plugin.tabListHandler.updateTabListForPlayer(event.player)
    }
}
