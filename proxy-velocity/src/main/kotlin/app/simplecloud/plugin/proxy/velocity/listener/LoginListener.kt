package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import app.simplecloud.plugin.proxy.shared.handler.ProxyJoinGate
import app.simplecloud.plugin.proxy.velocity.ProxyVelocityPlugin
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.ResultedEvent
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import kotlinx.coroutines.runBlocking

class LoginListener(
    private val proxyPlugin: ProxyPlugin,
    private val plugin: ProxyVelocityPlugin
) {

    @Subscribe(order = PostOrder.EARLY)
    fun handle(event: LoginEvent) {
        val player = event.player
        val result = runBlocking {
            proxyPlugin.proxyJoinGate.evaluate(player.username) { permission -> player.hasPermission(permission) }
        }

        if (result is ProxyJoinGate.Result.Denied) {
            event.result = ResultedEvent.ComponentResult.denied(
                plugin.deserializeToComponent(result.kickMessage, player)
            )
        }
    }
}
