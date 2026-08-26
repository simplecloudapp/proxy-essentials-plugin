package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import app.simplecloud.plugin.proxy.shared.joinstate.ProxyJoinGate
import app.simplecloud.plugin.proxy.velocity.ProxyVelocityPlugin
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import com.velocitypowered.api.proxy.Player
import kotlinx.coroutines.runBlocking
import java.util.logging.Logger

class ServerPreConnectListener(
    private val proxyPlugin: ProxyPlugin,
    private val plugin: ProxyVelocityPlugin
) {

    private val logger = Logger.getLogger(ServerPreConnectListener::class.java.name)

    @Subscribe(order = PostOrder.EARLY)
    fun handle(event: ServerPreConnectEvent) {
        if (event.previousServer == null) {
            checkAllowProxyJoin(event)
            if (!event.result.isAllowed) {
                return
            }
        }

        checkAllowServerSwitch(event)
    }

    private fun checkAllowProxyJoin(event: ServerPreConnectEvent) {
        val player = event.player
        val result = runBlocking {
            proxyPlugin.proxyJoinGate.evaluate(player.username) { permission -> player.hasPermission(permission) }
        }

        if (result is ProxyJoinGate.Result.Denied) {
            disconnect(player, result.kickMessage, event)
        }
    }

    private fun checkAllowServerSwitch(event: ServerPreConnectEvent) {
        val player = event.player
        val serverName = event.originalServer.serverInfo.name
        val resolver = proxyPlugin.joinStateResolver

        runBlocking {
            val joinStateName = resolver.getJoinStateForServer(serverName)
            val joinState = resolver.resolveJoinState(joinStateName)
            val kickMessages = proxyPlugin.messageConfig.get().kick

            if (joinState == null) {
                logger.severe("Neither join state '$joinStateName' nor default state found. Check configuration!")
                denyServerSwitch(player, kickMessages.noJoinState, event)
                return@runBlocking
            }

            val joinPermission = joinState.permission.join
            if (joinPermission.isNotBlank() && !player.hasPermission(joinPermission)) {
                logger.info("Player ${player.username} does not have permission to join $serverName. (JoinState: $joinStateName, Permission: $joinPermission)")
                denyServerSwitch(player, kickMessages.noPermission, event)
            }
        }
    }

    private fun denyServerSwitch(player: Player, message: String, event: ServerPreConnectEvent) {
        player.sendMessage(plugin.deserializeToComponent(message))
        event.result = ServerPreConnectEvent.ServerResult.denied()
    }

    private fun disconnect(player: Player, message: String, event: ServerPreConnectEvent) {
        player.disconnect(plugin.deserializeToComponent(message, player))
        event.result = ServerPreConnectEvent.ServerResult.denied()
    }
}
