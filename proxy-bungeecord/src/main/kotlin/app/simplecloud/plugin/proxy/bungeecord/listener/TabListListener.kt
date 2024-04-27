package app.simplecloud.plugin.proxy.bungeecord.listener

import app.simplecloud.plugin.proxy.bungeecord.ProxyBungeeCordPlugin
import net.md_5.bungee.api.event.ServerConnectedEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler

class TabListListener(
    private val plugin: ProxyBungeeCordPlugin
): Listener {

    @EventHandler
    fun onServerConnected(event: ServerConnectedEvent) {
        val player = event.player
        this.plugin.tabListHandler.updateTabListForPlayer(player)
    }

}