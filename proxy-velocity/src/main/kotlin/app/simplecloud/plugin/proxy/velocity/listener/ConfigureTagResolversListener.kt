package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.plugin.proxy.shared.config.motd.MaxPlayerDisplayType
import app.simplecloud.plugin.proxy.velocity.ProxyVelocityPlugin
import app.simplecloud.plugin.proxy.velocity.event.ConfigureTagResolversEvent
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.jvm.optionals.getOrNull

class ConfigureTagResolversListener(
    private val plugin: ProxyVelocityPlugin
) {

    @Subscribe(order = PostOrder.LAST)
    fun onConfigureTagResolvers(event: ConfigureTagResolversEvent) {
        val player = event.player
        if (player != null) {
            val serverName = player.currentServer.getOrNull()?.serverInfo?.name?: "unknown"
            val now = LocalDateTime.now(ZoneId.systemDefault())

            val ping = player.ping
            val pingColors = plugin.placeHolderConfiguration.pingColors
            val pingColor = pingColors.firstOrNull { it.ping >= ping }?.color ?: "<dark_red>"

            event.withTagResolvers(
                Placeholder.unparsed("server_name", serverName),
                Placeholder.parsed("ping", "$pingColor$ping"),
                Formatter.date("date", now),
            )
        }

        val onlinePlayers = this.plugin.proxyServer.allPlayers.size
        val realMaxPlayers = this.plugin.proxyServer.configuration.showMaxPlayers
        val maxPlayers = when (this.plugin.motdConfiguration.maxPlayerDisplayType) {
            MaxPlayerDisplayType.REAL -> realMaxPlayers
            MaxPlayerDisplayType.DYNAMIC -> onlinePlayers + this.plugin.motdConfiguration.dynamicPlayerRange
            null -> realMaxPlayers
        }

        event.withTagResolvers(
            Placeholder.unparsed("online_players", onlinePlayers.toString()),
            Placeholder.unparsed("max_players", maxPlayers.toString())
        )
    }

}
