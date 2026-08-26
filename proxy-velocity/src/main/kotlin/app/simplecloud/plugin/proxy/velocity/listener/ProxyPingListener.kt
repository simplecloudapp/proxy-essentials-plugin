package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import app.simplecloud.plugin.proxy.shared.config.MotdLayoutConfiguration
import app.simplecloud.plugin.proxy.shared.motd.ServerIconLoader
import app.simplecloud.plugin.proxy.shared.utilities.LocalPingSourceMatcher
import app.simplecloud.plugin.proxy.velocity.ProxyVelocityPlugin
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.proxy.server.ServerPing
import com.velocitypowered.api.util.Favicon
import java.nio.file.Path
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

class ProxyPingListener(
    private val proxyPlugin: ProxyPlugin,
    private val plugin: ProxyVelocityPlugin
) {

    private val serverIconLoader = ServerIconLoader(Path.of(proxyPlugin.serverIconsPath)) { image ->
        Favicon.create(image)
    }

    @Subscribe
    fun onProxyPing(event: ProxyPingEvent) {
        val layout = resolveLayout(event.connection.virtualHost.getOrNull()?.hostName)
        if (!layout.motd.enabled) return

        val entry = proxyPlugin.layoutRepository.selectEntry(layout, layout.configVersion) ?: return
        val builder = event.ping.asBuilder()
            .description(plugin.deserializeMotd(entry.line1, entry.line2))

        applyServerIcon(builder, layout)

        // The cloud pings the proxy itself to check its health, which must not affect the counts.
        if (!LocalPingSourceMatcher.isLocal(event.connection.remoteAddress.address)) {
            applyPlayerList(builder, layout, event.ping)
        }

        event.ping = builder.build()
    }

    private fun resolveLayout(virtualHost: String?): MotdLayoutConfiguration {
        val layoutName = when (virtualHost) {
            null -> null
            else -> proxyPlugin.domainMotdHandler.getLayoutNameForDomain(virtualHost)
        }
        val domainLayout = when (layoutName) {
            null -> null
            else -> proxyPlugin.layoutRepository.getLayoutByName(layoutName)
        }

        return domainLayout ?: proxyPlugin.layoutRepository.getCurrentMotdLayout()
    }

    private fun applyServerIcon(builder: ServerPing.Builder, layout: MotdLayoutConfiguration) {
        if (!layout.serverIcon.enabled) return

        val favicon = serverIconLoader.get(layout.serverIcon.file) { plugin.logger.warn(it) } ?: return
        builder.favicon(favicon)
    }

    private fun applyPlayerList(
        builder: ServerPing.Builder,
        layout: MotdLayoutConfiguration,
        ping: ServerPing
    ) {
        val playerCountHandler = proxyPlugin.playerCountTracker
        val onlinePlayers = playerCountHandler.onlinePlayers(plugin.server.allPlayers.size)
        val realMaxPlayers = playerCountHandler.maxPlayers(plugin.server.configuration.showMaxPlayers)

        builder
            .onlinePlayers(onlinePlayers)
            .maximumPlayers(layout.version.slots.resolveMaxPlayers(onlinePlayers, realMaxPlayers))
            .samplePlayers(*samplePlayers(layout, ping).toTypedArray())

        if (layout.version.name.enabled) {
            builder.version(ServerPing.Version(ping.version.protocol, layout.version.name.text))
        }
    }

    private fun samplePlayers(layout: MotdLayoutConfiguration, ping: ServerPing): List<ServerPing.SamplePlayer> {
        if (!layout.playerList.enabled || layout.playerList.playerList.isEmpty()) {
            return ping.players.getOrNull()?.sample ?: emptyList()
        }

        return layout.playerList.playerList.map { ServerPing.SamplePlayer(it, UUID.randomUUID()) }
    }
}