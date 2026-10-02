package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.plugin.proxy.shared.ProxyEssentials
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.KickedFromServerEvent

class ServerKickListener(
    private val essentials: ProxyEssentials
) {

    @Subscribe(priority = Short.MIN_VALUE)
    fun onKickedFromServer(event: KickedFromServerEvent) {
        if (!essentials.config.get().showKickReason) return
        if (event.result !is KickedFromServerEvent.DisconnectPlayer) return

        event.serverKickReason.ifPresent { reason ->
            event.result = KickedFromServerEvent.DisconnectPlayer.create(reason)
        }
    }
}
