package app.simplecloud.plugin.proxy.bungeecord.listener

import app.simplecloud.plugin.proxy.bungeecord.ProxyBungeeCordPlugin
import app.simplecloud.plugin.proxy.bungeecord.toBaseComponent
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

        val localState = this.proxyPlugin.proxyPlugin.joinStateHandler.localState
        val joinState = this.proxyPlugin.proxyPlugin.joinStateConfiguration.joinStates.find { it.name == localState }

        if (joinState == null) {
            logger.warning("No join state found for server.")
            denyAccess(player, proxyPlugin.proxyPlugin.messagesConfiguration.kickMessage.noJoinState, event)
            return
        }

        if (player.hasPermission(joinState.joinPermission)) {
            denyAccess(
                player,
                this.proxyPlugin.proxyPlugin.messagesConfiguration.kickMessage.networkMaintenance,
                event
            )
            return
        }

        runBlocking {
            try {
                if (!isServerFull()) {
                    return@runBlocking
                }

                if (player.hasPermission(joinState.fullJoinPermission)) {
                    return@runBlocking
                }
                denyAccess(player, proxyPlugin.proxyPlugin.messagesConfiguration.kickMessage.networkFull, event)
            } catch (e: Exception) {
                logger.severe("Error checking player limits: ${e.message}")
            }
        }
    }

    private suspend fun isServerFull(): Boolean {
        val groupName = this.proxyPlugin.proxyPlugin.cloudControllerHandler.groupName

        if (groupName == null) {
            logger.warning("No group name found for server.")
            return true
        }

        val maxPlayers = proxyPlugin.proxyPlugin.cloudControllerHandler.getMaxPlayersInGroup(groupName)
        val onlinePlayers = proxyPlugin.proxyPlugin.cloudControllerHandler.getOnlinePlayersInGroup(groupName)

        return onlinePlayers >= maxPlayers
    }

    private fun denyAccess(player: ProxiedPlayer, message: String, event: ServerConnectEvent) {
        player.disconnect(proxyPlugin.deserializeToComponent(message, player).toBaseComponent())
        event.isCancelled = true
    }
}
