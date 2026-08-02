package app.simplecloud.plugin.proxy.bungeecord.listener

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import net.md_5.bungee.api.event.ServerKickEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import net.md_5.bungee.event.EventPriority

class ServerKickListener internal constructor(
    private val showKickReason: () -> Boolean
) : Listener {

    constructor(proxyPlugin: ProxyPlugin) : this({
        proxyPlugin.proxyEssentialsConfig.get().showKickReason
    })

    @EventHandler(priority = EventPriority.HIGHEST)
    fun handle(event: ServerKickEvent) {
        if (!showKickReason() || event.reason == null) return

        event.isCancelled = false
        event.cancelServer = null
    }
}
