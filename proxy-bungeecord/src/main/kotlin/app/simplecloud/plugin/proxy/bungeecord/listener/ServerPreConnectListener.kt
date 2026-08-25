package app.simplecloud.plugin.proxy.bungeecord.listener

import app.simplecloud.plugin.proxy.bungeecord.ProxyBungeeCordPlugin
import app.simplecloud.plugin.proxy.bungeecord.toBaseComponent
import app.simplecloud.plugin.proxy.shared.handler.ProxyJoinGate
import kotlinx.coroutines.runBlocking
import net.md_5.bungee.api.config.ServerInfo
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.event.ServerConnectEvent
import net.md_5.bungee.api.event.ServerConnectEvent.Reason
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
        if (isProxyJoin(event)) {
            checkAllowProxyJoin(event.player, event)
            if (event.isCancelled) {
                return
            }
        }

        checkAllowServerSwitch(event.player, event, event.target)
    }

    private fun isProxyJoin(event: ServerConnectEvent): Boolean {
        return event.reason == Reason.JOIN_PROXY || event.player.server == null
    }

    private fun checkAllowProxyJoin(player: ProxiedPlayer, event: ServerConnectEvent) {
        val result = runBlocking {
            plugin.proxyPlugin.proxyJoinGate.evaluate(player.name) { permission -> player.hasPermission(permission) }
        }

        if (result is ProxyJoinGate.Result.Denied) {
            denyAccess(player, result.kickMessage, false, event)
        }
    }

    private fun checkAllowServerSwitch(player: ProxiedPlayer, event: ServerConnectEvent, server: ServerInfo) {
        val serverName = server.name

        runBlocking {
            val joinStateName = resolver.getJoinStateForServer(serverName)
            val joinState = resolver.resolveJoinState(joinStateName)

            if (joinState == null) {
                logger.severe("Neither join state '$joinStateName' nor default state found. Check configuration!")
                denyAccess(player, plugin.proxyPlugin.messagesConfiguration.get().kick.noJoinState, true, event)
                return@runBlocking
            }

            if (joinState.permission.join.isNotBlank() && !player.hasPermission(joinState.permission.join)) {
                logger.info("Player ${player.name} does not have permission to join $serverName.")
                denyAccess(player, plugin.proxyPlugin.messagesConfiguration.get().kick.noPermission, true, event)
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
