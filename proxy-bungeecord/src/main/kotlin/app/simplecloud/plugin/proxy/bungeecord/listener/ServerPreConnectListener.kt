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
    private val plugin: ProxyBungeeCordPlugin,
) : Listener {
    private val logger = Logger.getLogger(ServerPreConnectListener::class.java.name)
    private val resolver = plugin.proxyPlugin.joinStateResolver

    @EventHandler(priority = EventPriority.HIGH)
    fun handle(event: ServerConnectEvent) {
        checkAllowProxyJoin(event.player, event)
        checkAllowServerSwitch(event.player, event, event.target)
    }

    private fun checkAllowProxyJoin(player: ProxiedPlayer, event: ServerConnectEvent) {
        val localState = plugin.proxyPlugin.joinStateHandler.localState
        val joinState = resolver.resolveJoinState(localState)

        if (joinState == null) {
            logger.severe("Neither join state '$localState' nor default state found. Check configuration!")
            denyAccess(player, plugin.proxyPlugin.messagesConfiguration.get().kickMessage.noJoinState, false, event)
            return
        }

        if (joinState.joinPermission.isNotBlank() && !player.hasPermission(joinState.joinPermission)) {
            logger.info("Player ${player.name} does not have permission to join the proxy.")
            denyAccess(player, plugin.proxyPlugin.messagesConfiguration.get().kickMessage.noPermission, false, event)
            return
        }

        runBlocking {
            try {
                if (resolver.isServerFull() && !player.hasPermission(joinState.fullJoinPermission)) {
                    denyAccess(player, plugin.proxyPlugin.messagesConfiguration.get().kickMessage.networkFull, false, event)
                }
            } catch (e: Exception) {
                logger.severe("Error checking player limits: ${e.message}")
            }
        }
    }

    private fun checkAllowServerSwitch(player: ProxiedPlayer, event: ServerConnectEvent, server: ServerInfo) {
        val serverName = server.name

        runBlocking {
            val joinStateName = resolver.getJoinStateForServer(serverName)
            val joinState = resolver.resolveJoinState(joinStateName)

            if (joinState == null) {
                logger.severe("Neither join state '$joinStateName' nor default state found. Check configuration!")
                denyAccess(player, plugin.proxyPlugin.messagesConfiguration.get().kickMessage.noJoinState, true, event)
                return@runBlocking
            }

            if (joinState.joinPermission.isNotBlank() && !player.hasPermission(joinState.joinPermission)) {
                logger.info("Player ${player.name} does not have permission to join $serverName.")
                denyAccess(player, plugin.proxyPlugin.messagesConfiguration.get().kickMessage.noPermission, true, event)
            }
        }
    }

    private fun denyAccess(player: ProxiedPlayer, message: String, isSubServer: Boolean, event: ServerConnectEvent) {
        if (isSubServer) {
            player.sendMessage(plugin.deserializeToComponent(message, player).toBaseComponent())
        } else {
            player.disconnect(plugin.deserializeToComponent(message, player).toBaseComponent())
        }
        event.isCancelled = true
    }
}
