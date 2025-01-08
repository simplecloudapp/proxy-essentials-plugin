package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.plugin.api.shared.pattern.ServerPatternIdentifier
import app.simplecloud.plugin.proxy.velocity.ProxyVelocityPlugin
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.server.RegisteredServer
import kotlinx.coroutines.runBlocking
import java.util.logging.Logger

class ServerPreConnectListener(
    private val proxyPlugin: ProxyVelocityPlugin,
) {
    private val logger = Logger.getLogger(ServerPreConnectListener::class.java.name)

    private val identifier = ServerPatternIdentifier(
        this.proxyPlugin.joinStateConfiguration.serverNamePattern
    )

    @Subscribe(order = PostOrder.EARLY)
    fun handle(event: ServerPreConnectEvent) {
        val player = event.player
        val originalServer = event.originalServer

        checkAllowProxyJoin(player, event)
        checkAllowServerSwitch(player, event, originalServer)
    }

    private fun checkAllowProxyJoin(player: Player, event: ServerPreConnectEvent) {
        val localState = this.proxyPlugin.joinStateHandler.localState
        val joinState = this.proxyPlugin.joinStateConfiguration.joinStates.find { it.name == localState }

        if (joinState == null) {
            logger.info("The join state for the proxy could not be found.")
            denyAccess(player, proxyPlugin.messagesConfiguration.kickMessage.noJoinState, false, event)
            return
        }

        if (!player.hasPermission(joinState.joinPermission) && joinState.joinPermission.trim().isNotEmpty()) {
            logger.info("The player ${player.username} does not have the permission to join the proxy and will be kicked.")
            denyAccess(
                player,
                this.proxyPlugin.messagesConfiguration.kickMessage.noPermission,
                false,
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
                denyAccess(player, proxyPlugin.messagesConfiguration.kickMessage.networkFull, false, event)
                return@runBlocking
            } catch (e: Exception) {
                logger.severe("Error checking player limits: ${e.message}")
            }
        }
    }

    private fun checkAllowServerSwitch(player: Player, event: ServerPreConnectEvent, server: RegisteredServer) {
        val serverName = server.serverInfo.name

        val (groupName, numericalId) = identifier.parse(serverName)

        runBlocking {
            val joinStateName =
                proxyPlugin.joinStateHandler.getJoinStateAtService(groupName, numericalId.toLong())
            val joinState = proxyPlugin.joinStateConfiguration.joinStates.find { it.name == joinStateName }

            if (joinState == null) {
                logger.warning("The join state for the server $serverName could not be found.")
                denyAccess(player, proxyPlugin.messagesConfiguration.kickMessage.noJoinState, true, event)
                return@runBlocking
            }

            if (joinState.joinPermission.trim().isNotEmpty() && !player.hasPermission(joinState.joinPermission)) {
                logger.info("The player ${player.username} does not have the permission to join $serverName and will be kicked.")
                denyAccess(
                    player,
                    proxyPlugin.messagesConfiguration.kickMessage.noPermission,
                    true,
                    event
                )
                return@runBlocking
            }
        }
    }

    private suspend fun isServerFull(): Boolean {
        val groupName = this.proxyPlugin.cloudControllerHandler.groupName

        if (groupName == null) {
            logger.warning("No group name found for server.")
            return true
        }

        val maxPlayers = proxyPlugin.cloudControllerHandler.getMaxPlayersInGroup(groupName)
        val onlinePlayers = proxyPlugin.cloudControllerHandler.getOnlinePlayersInGroup(groupName)

        return onlinePlayers >= maxPlayers
    }

    private fun denyAccess(player: Player, message: String, sebServer: Boolean, event: ServerPreConnectEvent) {
        if (sebServer) {
            player.sendMessage(proxyPlugin.deserializeToComponent(message))
        } else {
            player.disconnect(proxyPlugin.deserializeToComponent(message, player))
        }
        event.result = ServerPreConnectEvent.ServerResult.denied()
    }
}
