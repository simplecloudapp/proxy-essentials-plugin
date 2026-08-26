package app.simplecloud.plugin.proxy.bungeecord.placeholder

import app.simplecloud.plugin.proxy.bungeecord.ProxyBungeeCordPlugin
import app.simplecloud.plugin.proxy.shared.placeholder.TagResolverHelper
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import net.md_5.bungee.event.EventPriority

class ConfigureTagResolversListener(
    private val plugin: ProxyBungeeCordPlugin
) : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onConfigureTagResolvers(event: ConfigureTagResolversEvent) {
        val proxyPlugin = plugin.proxyPlugin
        val player = event.player
        val playerCountHandler = proxyPlugin.playerCountTracker

        event.withTagResolvers(
            TagResolverHelper.getDefaultTagResolvers(
                serverName = player?.server?.info?.name ?: "unknown",
                ping = (player?.ping ?: -1).toLong(),
                pingColors = proxyPlugin.placeholderConfig.get().pingColors,
                onlinePlayers = playerCountHandler.onlinePlayers(plugin.proxy.players.size),
                realMaxPlayers = playerCountHandler.maxPlayers(plugin.proxy.config.playerLimit),
                motdConfiguration = proxyPlugin.layoutRepository.getCurrentMotdLayout()
            )
        )
    }
}
