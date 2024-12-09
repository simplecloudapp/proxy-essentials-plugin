package app.simplecloud.plugin.proxy.bungeecord.listener

import app.simplecloud.plugin.proxy.bungeecord.ProxyBungeeCordPlugin
import app.simplecloud.plugin.proxy.bungeecord.event.ConfigureTagResolversEvent
import app.simplecloud.plugin.proxy.shared.resolver.TagResolverHelper
import kotlinx.coroutines.runBlocking
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import net.md_5.bungee.event.EventPriority

class ConfigureTagResolversListener(
    private val plugin: ProxyBungeeCordPlugin
) : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onConfigureTagResolvers(event: ConfigureTagResolversEvent) {

        runBlocking {
            val player = event.player
            val serverName = player?.server?.info?.name ?: "unknown"

            val ping = player?.ping ?: -1
            val pingColors = plugin.proxyPlugin.placeHolderConfiguration.pingColors

            val onlinePlayers = plugin.proxy.players.size
            val realMaxPlayers = plugin.proxy.config.playerLimit

            event.withTagResolvers(
                TagResolverHelper.getDefaultTagResolvers(
                    serverName,
                    ping.toLong(),
                    pingColors,
                    onlinePlayers,
                    realMaxPlayers,
                    plugin.proxyPlugin.motdLayoutHandler.getCurrentMotdLayout()
                )
            )
        }
    }

}
