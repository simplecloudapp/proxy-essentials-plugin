package app.simplecloud.plugin.proxy.bungeecord.listener

import app.simplecloud.plugin.proxy.shared.ProxyEssentials
import net.md_5.bungee.api.event.ServerKickEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler

class ServerKickListener(
    private val essentials: ProxyEssentials
) : Listener {

    @EventHandler(priority = Byte.MAX_VALUE)
    fun onServerKick(event: ServerKickEvent) {
        if (!essentials.config.get().showKickReason) return
        if (event.cancelServer != null) return

        val reason = event.reason ?: return
        event.player.disconnect(reason)
    }
}
