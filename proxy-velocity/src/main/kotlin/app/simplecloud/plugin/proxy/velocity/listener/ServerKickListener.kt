package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.KickedFromServerEvent

class ServerKickListener internal constructor(
    private val showKickReason: () -> Boolean
) {

    constructor(proxyPlugin: ProxyPlugin) : this({
        proxyPlugin.proxyEssentialsConfig.get().showKickReason
    })

    @Subscribe(priority = Short.MAX_VALUE)
    fun handle(event: KickedFromServerEvent) {
        if (!showKickReason()) return

        event.serverKickReason.ifPresent { serverReason ->
            event.result = KickedFromServerEvent.DisconnectPlayer.create(serverReason)
            event.player.disconnect(serverReason)
        }
    }
}
