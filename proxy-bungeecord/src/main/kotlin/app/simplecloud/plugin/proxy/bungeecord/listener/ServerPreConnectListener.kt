package app.simplecloud.plugin.proxy.bungeecord.listener

import app.simplecloud.plugin.proxy.bungeecord.ProxyBungeeCordPlugin
import app.simplecloud.plugin.proxy.bungeecord.toBaseComponent
import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import kotlinx.coroutines.runBlocking
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.event.ServerConnectEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import net.md_5.bungee.event.EventPriority
import java.util.logging.Logger

class ServerPreConnectListener(
    private val proxyPlugin: ProxyBungeeCordPlugin,
): Listener {
    private val logger = Logger.getLogger(ServerPreConnectListener::class.java.name)

    @EventHandler(priority = EventPriority.HIGH)
    fun handle(event: ServerConnectEvent) {
        val player = event.player

        if (proxyPlugin.proxyPlugin.maintenance && !player.hasPermission(ProxyPlugin.JOIN_MAINTENANCE_PERMISSION)) {
            denyAccess(
                player,
                this.proxyPlugin.proxyPlugin.messagesConfiguration.kickMessage.networkMaintenance,
                event
            )
            return
        }

        runBlocking {
            try {
                if (!isServerFull(player)) {
                    return@runBlocking
                }
                denyAccess(player, proxyPlugin.proxyPlugin.messagesConfiguration.kickMessage.networkFull, event)
            } catch (e: Exception) {
                logger.severe("Error checking player limits: ${e.message}")
            }
        }
    }

    private suspend fun isServerFull(player: ProxiedPlayer): Boolean {
        val maxPlayers = proxyPlugin.proxyPlugin.cloudControllerHandler.getMaxPlayersInGroup()
        val onlinePlayers = proxyPlugin.proxyPlugin.cloudControllerHandler.getOnlinePlayersInGroup()

        return onlinePlayers >= maxPlayers && !player.hasPermission(ProxyPlugin.JOIN_FULL_PERMISSION)
    }

    private fun denyAccess(player: ProxiedPlayer, message: String, event: ServerConnectEvent) {
        player.disconnect(proxyPlugin.deserializeToComponent(message, player).toBaseComponent())
        event.isCancelled = true
    }
}
