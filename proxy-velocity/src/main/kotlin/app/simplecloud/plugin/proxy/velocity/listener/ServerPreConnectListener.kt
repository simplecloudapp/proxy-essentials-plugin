package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import app.simplecloud.plugin.proxy.velocity.ProxyVelocityPlugin
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import com.velocitypowered.api.proxy.Player
import kotlinx.coroutines.runBlocking
import java.util.logging.Logger

class ServerPreConnectListener(
    private val proxyPlugin: ProxyVelocityPlugin,
) {
    private val logger = Logger.getLogger(ServerPreConnectListener::class.java.name)

    @Subscribe(order = PostOrder.EARLY)
    fun handle(event: ServerPreConnectEvent) {
        val player = event.player

        if (proxyPlugin.maintenance && !player.hasPermission(ProxyPlugin.JOIN_MAINTENANCE_PERMISSION)) {
            denyAccess(
                player,
                this.proxyPlugin.messagesConfiguration.kickMessage.networkMaintenance,
                event
            )
            return
        }

        runBlocking {
            try {
                if (!isServerFull(player)) {
                    return@runBlocking
                }
                denyAccess(player, proxyPlugin.messagesConfiguration.kickMessage.networkFull, event)
            } catch (e: Exception) {
                logger.severe("Error checking player limits: ${e.message}")
            }
        }
    }

    private suspend fun isServerFull(player: Player): Boolean {
        val maxPlayers = proxyPlugin.cloudControllerHandler.getMaxPlayersInGroup()
        val onlinePlayers = proxyPlugin.cloudControllerHandler.getOnlinePlayersInGroup()

        return onlinePlayers >= maxPlayers && !player.hasPermission(ProxyPlugin.JOIN_FULL_PERMISSION)
    }

    private fun denyAccess(player: Player, message: String, event: ServerPreConnectEvent) {
        player.disconnect(proxyPlugin.deserializeToComponent(message, player))
        event.result = ServerPreConnectEvent.ServerResult.denied()
    }
}
