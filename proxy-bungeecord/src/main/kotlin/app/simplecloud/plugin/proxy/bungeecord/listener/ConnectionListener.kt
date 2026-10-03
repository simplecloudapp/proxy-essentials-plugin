package app.simplecloud.plugin.proxy.bungeecord.listener

import app.simplecloud.plugin.proxy.bungeecord.platform.BungeeProxyPlayer
import app.simplecloud.plugin.proxy.shared.ProxyEssentials
import app.simplecloud.plugin.proxy.shared.joinstate.JoinResult
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer
import net.md_5.bungee.api.event.PostLoginEvent
import net.md_5.bungee.api.event.ServerConnectEvent
import net.md_5.bungee.api.event.ServerConnectedEvent
import net.md_5.bungee.api.event.ServerKickEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import net.md_5.bungee.event.EventPriority

class ConnectionListener(
    private val essentials: ProxyEssentials
) : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPostLogin(event: PostLoginEvent) {
        val player = event.player
        val result = essentials.joinGate.checkProxyJoin(player.name, player.uniqueId, player::hasPermission)
        if (result !is JoinResult.Denied) return

        val message = essentials.messageFormatter.formatForPlayer(result.message, "unknown", player.ping.toLong())
        player.disconnect(*BungeeComponentSerializer.get().serialize(message))
    }

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

    @EventHandler
    fun onServerConnected(event: ServerConnectedEvent) {
        essentials.tabListService.update(BungeeProxyPlayer(event.player))
    }

    @EventHandler(priority = Byte.MAX_VALUE)
    fun onServerKick(event: ServerKickEvent) {
        if (!essentials.config.get().showKickReason) return
        if (event.cancelServer != null) return

        val reason = event.reason ?: return
        event.player.disconnect(reason)
    }

}