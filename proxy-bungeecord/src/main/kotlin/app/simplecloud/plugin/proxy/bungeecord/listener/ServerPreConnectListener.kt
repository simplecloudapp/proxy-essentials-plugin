package app.simplecloud.plugin.proxy.bungeecord.listener

import app.simplecloud.plugin.proxy.bungeecord.ProxyBungeeCordPlugin
import app.simplecloud.plugin.proxy.bungeecord.toBaseComponent
import app.simplecloud.plugin.proxy.shared.joinstate.ProxyJoinGate
import kotlinx.coroutines.runBlocking
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.event.ServerConnectEvent
import net.md_5.bungee.api.event.ServerConnectEvent.Reason
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import net.md_5.bungee.event.EventPriority
import org.slf4j.LoggerFactory

class ServerPreConnectListener(
    private val plugin: ProxyBungeeCordPlugin
) : Listener {

    private val logger = LoggerFactory.getLogger(ServerPreConnectListener::class.java)
    private val proxyPlugin = plugin.proxyPlugin

    @EventHandler(priority = EventPriority.HIGH)
    fun handle(event: ServerConnectEvent) {
        if (isProxyJoin(event)) {
            checkAllowProxyJoin(event)
            if (event.isCancelled) {
                return
            }
        }

        checkAllowServerSwitch(event)
    }

    private fun isProxyJoin(event: ServerConnectEvent): Boolean {
        return event.reason == Reason.JOIN_PROXY || event.player.server == null
    }

    private fun checkAllowProxyJoin(event: ServerConnectEvent) {
        val player = event.player
        val result = runBlocking {
            proxyPlugin.proxyJoinGate.evaluate(player.name) { permission -> player.hasPermission(permission) }
        }

        if (result is ProxyJoinGate.Result.Denied) {
            disconnect(player, result.kickMessage, event)
        }
    }

    private fun checkAllowServerSwitch(event: ServerConnectEvent) {
        val player = event.player
        val serverName = event.target.name
        val resolver = proxyPlugin.joinStateResolver

        runBlocking {
            val joinStateName = resolver.getJoinStateForServer(serverName)
            val joinState = resolver.resolveJoinState(joinStateName)
            val kickMessages = proxyPlugin.messageConfig.get().kick

            if (joinState == null) {
                logger.error("Neither join state '$joinStateName' nor default state found. Check configuration!")
                denyServerSwitch(player, kickMessages.noJoinState, event)
                return@runBlocking
            }

            val joinPermission = joinState.permission.join
            if (joinPermission.isNotBlank() && !player.hasPermission(joinPermission)) {
                logger.info("Player ${player.name} does not have permission to join $serverName.")
                denyServerSwitch(player, kickMessages.noPermission, event)
            }
        }
    }

    private fun denyServerSwitch(player: ProxiedPlayer, message: String, event: ServerConnectEvent) {
        player.sendMessage(plugin.deserializeToComponent(message, player).toBaseComponent())
        event.isCancelled = true
    }

    private fun disconnect(player: ProxiedPlayer, message: String, event: ServerConnectEvent) {
        player.disconnect(plugin.deserializeToComponent(message, player).toBaseComponent())
        event.isCancelled = true
    }
}
