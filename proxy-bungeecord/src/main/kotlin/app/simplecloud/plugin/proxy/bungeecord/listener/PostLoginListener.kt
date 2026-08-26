package app.simplecloud.plugin.proxy.bungeecord.listener

import app.simplecloud.plugin.proxy.bungeecord.ProxyBungeeCordPlugin
import app.simplecloud.plugin.proxy.bungeecord.toBaseComponent
import app.simplecloud.plugin.proxy.shared.joinstate.ProxyJoinGate
import kotlinx.coroutines.runBlocking
import net.md_5.bungee.api.event.PostLoginEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import net.md_5.bungee.event.EventPriority

class PostLoginListener(
    private val plugin: ProxyBungeeCordPlugin
) : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun handle(event: PostLoginEvent) {
        val player = event.player
        val result = runBlocking {
            plugin.proxyPlugin.proxyJoinGate.evaluate(player.name) { permission -> player.hasPermission(permission) }
        }

        if (result is ProxyJoinGate.Result.Denied) {
            player.disconnect(plugin.deserializeToComponent(result.kickMessage, player).toBaseComponent())
        }
    }
}
