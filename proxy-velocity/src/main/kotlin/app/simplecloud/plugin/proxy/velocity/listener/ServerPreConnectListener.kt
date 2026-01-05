package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.plugin.proxy.velocity.ProxyVelocityPlugin
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.server.RegisteredServer
import kotlinx.coroutines.runBlocking
import java.util.logging.Logger

class ServerPreConnectListener(
    private val plugin: ProxyVelocityPlugin,
) {
    private val logger = Logger.getLogger(ServerPreConnectListener::class.java.name)
    private val resolver = plugin.joinStateResolver

    @Subscribe(order = PostOrder.EARLY)
    fun handle(event: ServerPreConnectEvent) {
        checkAllowProxyJoin(event.player, event)
        checkAllowServerSwitch(event.player, event, event.originalServer)
    }

    private fun checkAllowProxyJoin(player: Player, event: ServerPreConnectEvent) {
        val localState = plugin.joinStateHandler.localState
        val joinState = resolver.resolveJoinState(localState)

        if (joinState == null) {
            logger.severe("Neither join state '$localState' nor default state found. Check configuration!")
            denyAccess(player, plugin.messagesConfiguration.get().kickMessage.noJoinState, false, event)
            return
        }

        if (joinState.joinPermission.isNotBlank() && !player.hasPermission(joinState.joinPermission)) {
            logger.info("Player ${player.username} does not have permission to join the proxy.")
            denyAccess(player, plugin.messagesConfiguration.get().kickMessage.noPermission, false, event)
            return
        }

        runBlocking {
            try {
                if (resolver.isServerFull() && !player.hasPermission(joinState.fullJoinPermission)) {
                    denyAccess(player, plugin.messagesConfiguration.get().kickMessage.networkFull, false, event)
                }
            } catch (e: Exception) {
                logger.severe("Error checking player limits: ${e.message}")
            }
        }
    }

    private fun checkAllowServerSwitch(player: Player, event: ServerPreConnectEvent, server: RegisteredServer) {
        val serverName = server.serverInfo.name

        runBlocking {
            val joinStateName = resolver.getJoinStateForServer(serverName)
            val joinState = resolver.resolveJoinState(joinStateName)

            if (joinState == null) {
                logger.severe("Neither join state '$joinStateName' nor default state found. Check configuration!")
                denyAccess(player, plugin.messagesConfiguration.get().kickMessage.noJoinState, true, event)
                return@runBlocking
            }

            if (joinState.joinPermission.isNotBlank() && !player.hasPermission(joinState.joinPermission)) {
                logger.info("Player ${player.username} does not have permission to join $serverName.")
                denyAccess(player, plugin.messagesConfiguration.get().kickMessage.noPermission, true, event)
            }
        }
    }

    private fun denyAccess(player: Player, message: String, isSubServer: Boolean, event: ServerPreConnectEvent) {
        if (isSubServer) {
            player.sendMessage(plugin.deserializeToComponent(message))
        } else {
            player.disconnect(plugin.deserializeToComponent(message, player))
        }
        event.result = ServerPreConnectEvent.ServerResult.denied()
    }
}
