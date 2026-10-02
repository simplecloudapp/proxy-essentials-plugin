package app.simplecloud.plugin.proxy.bungeecord.listener

import app.simplecloud.plugin.proxy.bungeecord.TabListHandler
import net.md_5.bungee.api.event.ServerConnectedEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler

class TabListListener(
    private val tabListHandler: TabListHandler
) : Listener {

    @EventHandler
    fun onServerConnected(event: ServerConnectedEvent) {
        tabListHandler.update(event.player)
    }
}
