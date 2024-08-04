package app.simplecloud.plugin.proxy.bungeecord.listener

import app.simplecloud.plugin.proxy.bungeecord.ProxyBungeeCordPlugin
import app.simplecloud.plugin.proxy.bungeecord.event.ConfigureTagResolversEvent
import app.simplecloud.plugin.proxy.shared.resolver.TagResolverHelper
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import net.md_5.bungee.event.EventPriority

class ConfigureTagResolversListener(
    private val plugin: ProxyBungeeCordPlugin
) : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onConfigureTagResolvers(event: ConfigureTagResolversEvent) {
        val player = event.player
        val serverName = player?.server?.info?.name ?: "unknown"

        val ping = player?.ping?.toLong() ?: -1
        val pingColors = plugin.placeHolderConfiguration.pingColors

        val onlinePlayers = this.plugin.proxy.players.size
        val realMaxPlayers = this.plugin.proxy.config.listeners.sumOf { it.maxPlayers }

        event.withTagResolvers(
            TagResolverHelper.getDefaultTagResolvers(
                serverName,
                ping,
                pingColors,
                onlinePlayers,
                realMaxPlayers,
                this.plugin.motdLayoutHandler.getCurrentMotdLayout()
            )
        )
    }

}
