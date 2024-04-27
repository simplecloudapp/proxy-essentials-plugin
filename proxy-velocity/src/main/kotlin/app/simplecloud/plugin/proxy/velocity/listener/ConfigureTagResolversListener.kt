package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.plugin.proxy.shared.resolver.TagResolverHelper
import app.simplecloud.plugin.proxy.velocity.ProxyVelocityPlugin
import app.simplecloud.plugin.proxy.velocity.event.ConfigureTagResolversEvent
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import kotlin.jvm.optionals.getOrNull

class ConfigureTagResolversListener(
    private val plugin: ProxyVelocityPlugin
) {

    @Subscribe(order = PostOrder.FIRST)
    fun onConfigureTagResolvers(event: ConfigureTagResolversEvent) {
        val player = event.player
        val serverName = player?.currentServer?.getOrNull()?.serverInfo?.name ?: "unknown"

        val ping = player?.ping ?: -1
        val pingColors = plugin.placeHolderConfiguration.pingColors

        val onlinePlayers = this.plugin.proxyServer.allPlayers.size
        val realMaxPlayers = this.plugin.proxyServer.configuration.showMaxPlayers

        event.withTagResolvers(
            TagResolverHelper.getDefaultTagResolvers(
                serverName,
                ping,
                pingColors,
                onlinePlayers,
                realMaxPlayers,
                this.plugin.motdConfiguration
            )
        )
    }

}
