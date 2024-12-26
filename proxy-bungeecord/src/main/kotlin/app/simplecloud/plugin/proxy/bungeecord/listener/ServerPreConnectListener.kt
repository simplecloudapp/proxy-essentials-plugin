package app.simplecloud.plugin.proxy.bungeecord.listener

import app.simplecloud.plugin.proxy.bungeecord.ProxyBungeeCordPlugin
import app.simplecloud.plugin.proxy.bungeecord.toBaseComponent
import kotlinx.coroutines.runBlocking
import net.md_5.bungee.api.config.ServerInfo
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
        val serverInfo = event.target

        checkAllowProxyJoin(player, event)
        checkAllowServerSwitch(player, event, serverInfo)
    }

    private fun checkAllowProxyJoin(player: ProxiedPlayer, event: ServerConnectEvent) {
        val localState = this.proxyPlugin.proxyPlugin.joinStateHandler.localState
        val joinState = this.proxyPlugin.proxyPlugin.joinStateConfiguration.joinStates.find { it.name == localState }

        if (joinState == null) {
            logger.info("The join state for the proxy could not be found.")
            denyAccess(player, proxyPlugin.proxyPlugin.messagesConfiguration.kickMessage.noJoinState, false, event)
            return
        }

        if (!player.hasPermission(joinState.joinPermission) && joinState.joinPermission != "") {
            logger.info("The player ${player.name} does not have the permission to join the proxy and will be kicked.")
            denyAccess(
                player,
                this.proxyPlugin.proxyPlugin.messagesConfiguration.kickMessage.noPermission,
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
                denyAccess(player, proxyPlugin.proxyPlugin.messagesConfiguration.kickMessage.networkFull, false, event)
            } catch (e: Exception) {
                logger.severe("Error checking player limits: ${e.message}")
            }
        }
    }

    private fun checkAllowServerSwitch(player: ProxiedPlayer, event: ServerConnectEvent, server: ServerInfo) {
        val serviceName = server.name
        val split = serviceName.split("-")

        val numericalId = split.last()

        val mutableList = split.toMutableList()
        mutableList.removeLast()
        val groupName = mutableList.joinToString("-")

        runBlocking {
            val joinStateName =
                proxyPlugin.proxyPlugin.joinStateHandler.getJoinStateAtService(groupName, numericalId.toLong())
            val joinState = proxyPlugin.proxyPlugin.joinStateConfiguration.joinStates.find { it.name == joinStateName }

            if (joinState == null) {
                logger.warning("The join state for the server $serviceName could not be found.")
                denyAccess(player, proxyPlugin.proxyPlugin.messagesConfiguration.kickMessage.noJoinState, true, event)
                return@runBlocking
            }

            if (joinState.joinPermission != "" && !player.hasPermission(joinState.joinPermission)) {
                logger.info("The player ${player.name} does not have the permission to join $serviceName and will be kicked.")
                denyAccess(
                    player,
                    proxyPlugin.proxyPlugin.messagesConfiguration.kickMessage.noPermission,
                    true,
                    event
                )
                return@runBlocking
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

    private fun denyAccess(player: ProxiedPlayer, message: String, subServer: Boolean, event: ServerConnectEvent) {
        if (subServer) {
            player.sendMessage(proxyPlugin.deserializeToComponent(message, player).toBaseComponent())
        } else {
            player.disconnect(proxyPlugin.deserializeToComponent(message, player).toBaseComponent())
        }
        event.isCancelled = true
    }
}
