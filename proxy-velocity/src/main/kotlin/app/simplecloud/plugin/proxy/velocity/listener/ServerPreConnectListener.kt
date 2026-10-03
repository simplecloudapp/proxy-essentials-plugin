package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.plugin.proxy.shared.ProxyEssentials
import app.simplecloud.plugin.proxy.shared.joinstate.JoinResult
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import kotlin.jvm.optionals.getOrNull

class ServerPreConnectListener(
    private val essentials: ProxyEssentials
) {

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
}
