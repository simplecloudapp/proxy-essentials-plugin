package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.plugin.proxy.shared.ProxyEssentials
import app.simplecloud.plugin.proxy.velocity.platform.VelocityProxyPlayer
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.ServerPostConnectEvent

class TabListListener(
    private val essentials: ProxyEssentials
) {

    @Subscribe
    fun onServerPostConnect(event: ServerPostConnectEvent) {
        essentials.tabListService.update(VelocityProxyPlayer(event.player))
    }
}
