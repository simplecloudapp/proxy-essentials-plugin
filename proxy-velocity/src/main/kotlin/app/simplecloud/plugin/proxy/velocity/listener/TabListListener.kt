package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.plugin.proxy.velocity.TabListHandler
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.ServerPostConnectEvent

class TabListListener(
    private val tabListHandler: TabListHandler
) {

    @Subscribe
    fun onServerPostConnect(event: ServerPostConnectEvent) {
        tabListHandler.update(event.player)
    }
}
