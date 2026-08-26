package app.simplecloud.plugin.proxy.velocity.placeholder

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import app.simplecloud.plugin.proxy.shared.placeholder.TagResolverHelper
import app.simplecloud.plugin.proxy.velocity.ProxyVelocityPlugin
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import kotlin.jvm.optionals.getOrNull

class ConfigureTagResolversListener(
    private val proxyPlugin: ProxyPlugin,
    private val plugin: ProxyVelocityPlugin
) {

    @Subscribe(order = PostOrder.FIRST)
    fun onConfigureTagResolvers(event: ConfigureTagResolversEvent) {
        val player = event.player
        val playerCountHandler = proxyPlugin.playerCountTracker

        event.withTagResolvers(
            TagResolverHelper.getDefaultTagResolvers(
                serverName = player?.currentServer?.getOrNull()?.serverInfo?.name ?: "unknown",
                ping = player?.ping ?: -1,
                pingColors = proxyPlugin.placeholderConfig.get().pingColors,
                onlinePlayers = playerCountHandler.onlinePlayers(plugin.server.allPlayers.size),
                realMaxPlayers = playerCountHandler.maxPlayers(plugin.server.configuration.showMaxPlayers),
                motdConfiguration = proxyPlugin.layoutRepository.getCurrentMotdLayout()
            )
        )
    }
}
