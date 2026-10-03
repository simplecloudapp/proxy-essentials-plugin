package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.plugin.proxy.shared.ProxyEssentials
import app.simplecloud.plugin.proxy.shared.joinstate.JoinResult
import app.simplecloud.plugin.proxy.velocity.platform.VelocityProxyPlayer
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.ResultedEvent
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.player.KickedFromServerEvent
import com.velocitypowered.api.event.player.ServerPostConnectEvent
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import kotlin.jvm.optionals.getOrNull

class ConnectionListener(
    private val essentials: ProxyEssentials
) {

    @Subscribe(order = PostOrder.EARLY)
    fun onLogin(event: LoginEvent) {
        val player = event.player
        val result = essentials.joinGate.checkProxyJoin(player.username, player.uniqueId, player::hasPermission)
        if (result !is JoinResult.Denied) return

        val message = essentials.messageFormatter.formatForPlayer(result.message, "unknown", player.ping)
        event.result = ResultedEvent.ComponentResult.denied(message)
    }

    @Subscribe(order = PostOrder.LAST)
    fun onServerPreConnect(event: ServerPreConnectEvent) {
        val player = event.player
        val target = event.result.server.getOrNull() ?: return
        val serverName = target.serverInfo.name
        val result = essentials.joinGate.checkServerSwitch(player.username, player.uniqueId, serverName, player::hasPermission)
        if (result !is JoinResult.Denied) return

        val message = essentials.messageFormatter.formatForPlayer(result.message, serverName, player.ping)
        event.result = ServerPreConnectEvent.ServerResult.denied()

        if (event.previousServer == null) {
            player.disconnect(message)
        } else {
            player.sendMessage(message)
        }
    }

    @Subscribe
    fun onServerPostConnect(event: ServerPostConnectEvent) {
        essentials.tabListService.update(VelocityProxyPlayer(event.player))
    }

    @Subscribe(priority = Short.MIN_VALUE)
    fun onKickedFromServer(event: KickedFromServerEvent) {
        if (!essentials.config.get().showKickReason) return
        if (event.result !is KickedFromServerEvent.DisconnectPlayer) return

        event.serverKickReason.ifPresent { reason ->
            event.result = KickedFromServerEvent.DisconnectPlayer.create(reason)
        }
    }
}