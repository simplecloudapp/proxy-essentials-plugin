package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import app.simplecloud.plugin.proxy.velocity.ProxyVelocityPlugin
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.server.RegisteredServer
import kotlinx.coroutines.runBlocking
import java.util.logging.Logger

class ServerPreConnectListener(
    private val proxyPlugin: ProxyPlugin,
    private val plugin: ProxyVelocityPlugin
) {
    private val logger = Logger.getLogger(ServerPreConnectListener::class.java.name)
    private val resolver = proxyPlugin.joinStateResolver

    @Subscribe(order = PostOrder.EARLY)
    fun handle(event: ServerPreConnectEvent) {
        if (event.previousServer == null) {
            checkAllowProxyJoin(event.player, event)
            if (event.result.isAllowed.not()) {
                return
            }
        }

        checkAllowServerSwitch(event.player, event, event.originalServer)
    }

    private fun checkAllowProxyJoin(player: Player, event: ServerPreConnectEvent) {
        val localState = proxyPlugin.joinStateHandler.localState
        val joinState = resolver.resolveJoinState(localState)

        if (joinState == null) {
            logger.severe("Neither join state '$localState' nor default state found. Check configuration!")
            denyAccess(player, proxyPlugin.messagesConfiguration.get().kick.noJoinState, false, event)
            return
        }

        if (joinState.permission.join.isNotBlank() && !player.hasPermission(joinState.permission.join)) {
            logger.info("Player ${player.username} does not have permission to join the proxy.")
            denyAccess(player, proxyPlugin.messagesConfiguration.get().kick.noPermission, false, event)
            return
        }

        runBlocking {
            try {
                if (resolver.isServerFull() && !player.hasPermission(joinState.permission.full)) {
                    denyAccess(player, proxyPlugin.messagesConfiguration.get().kick.networkFull, false, event)
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
                denyAccess(player, proxyPlugin.messagesConfiguration.get().kick.noJoinState, true, event)
                return@runBlocking
            }

            if (joinState.permission.join.isNotBlank() && !player.hasPermission(joinState.permission.join)) {
                logger.info("Player ${player.username} does not have permission to join $serverName. (JoinState: ${joinStateName}, Permission: ${joinState.permission.join})")
                denyAccess(player, proxyPlugin.messagesConfiguration.get().kick.noPermission, true, event)
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
