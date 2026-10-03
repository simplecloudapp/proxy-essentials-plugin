package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.plugin.proxy.shared.ProxyEssentials
import app.simplecloud.plugin.proxy.shared.joinstate.JoinResult
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.ResultedEvent
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent

class LoginListener(
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
}
