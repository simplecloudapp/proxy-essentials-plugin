package app.simplecloud.plugin.proxy.bungeecord.listener

import app.simplecloud.plugin.proxy.shared.ProxyEssentials
import app.simplecloud.plugin.proxy.shared.joinstate.JoinResult
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer
import net.md_5.bungee.api.event.ServerConnectEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import net.md_5.bungee.event.EventPriority

class ServerPreConnectListener(
    private val essentials: ProxyEssentials
) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onServerConnect(event: ServerConnectEvent) {
        if (event.isCancelled) return

        val player = event.player
        val serverName = event.target.name
        val result = essentials.joinGate.checkServerSwitch(player.name, player.uniqueId, serverName, player::hasPermission)
        if (result !is JoinResult.Denied) return

        val message = essentials.messageFormatter.formatForPlayer(result.message, serverName, player.ping.toLong())
        val components = BungeeComponentSerializer.get().serialize(message)
        event.isCancelled = true

        if (player.server == null) {
            player.disconnect(*components)
        } else {
            player.sendMessage(*components)
        }
    }
}
