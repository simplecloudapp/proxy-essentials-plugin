package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.KickedFromServerEvent

class ServerKickListener(
    private val proxyPlugin: ProxyPlugin
) {

    @Subscribe(priority = Short.MIN_VALUE)
    fun handle(event: KickedFromServerEvent) {
        if (!proxyPlugin.config.get().showKickReason) return
        if (event.result !is KickedFromServerEvent.DisconnectPlayer) return

        event.serverKickReason.ifPresent { serverReason ->
            event.result = KickedFromServerEvent.DisconnectPlayer.create(serverReason)
        }
    }
}
