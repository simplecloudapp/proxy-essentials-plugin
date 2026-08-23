package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import app.simplecloud.plugin.proxy.shared.config.motd.MaxPlayerDisplayType
import app.simplecloud.plugin.proxy.shared.handler.LocalPingSourceMatcher
import app.simplecloud.plugin.proxy.shared.handler.ServerIconLoader
import app.simplecloud.plugin.proxy.velocity.ProxyVelocityPlugin
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.proxy.server.ServerPing
import com.velocitypowered.api.proxy.server.ServerPing.SamplePlayer
import com.velocitypowered.api.util.Favicon
import java.nio.file.Path
import java.util.*
import kotlin.jvm.optionals.getOrNull

class ProxyPingListener(
    private val proxyPlugin: ProxyPlugin,
    private val plugin: ProxyVelocityPlugin
) {

    private val serverIconLoader = ServerIconLoader(
        Path.of(proxyPlugin.serverIconsPath)
    ) { image -> Favicon.create(image) }
    private val localPingSourceMatcher = LocalPingSourceMatcher()

    @Subscribe
    fun onProxyPing(event: ProxyPingEvent) {
        val virtualHost = event.connection.virtualHost.getOrNull()?.hostName
        val layout = virtualHost
            ?.let { proxyPlugin.domainMotdHandler.getLayoutNameForDomain(it) }
            ?.let { proxyPlugin.motdLayoutHandler.getLayoutByName(it) }
            ?: proxyPlugin.motdLayoutHandler.getCurrentMotdLayout()

        if (!layout.motd.enabled) return

        val entry = proxyPlugin.motdLayoutHandler.selectEntry(layout, layout.configVersion)
            ?: return
        val motd = plugin.deserializeMotd(entry.line1, entry.line2)
        val isLocalPing = localPingSourceMatcher.isLocal(event.connection.remoteAddress.address)
        val builder = event.ping.asBuilder().description(motd)

        // server icon
        if (layout.serverIcon.enabled) {
            serverIconLoader.get(layout.serverIcon.file) { plugin.logger.warn(it) }
                ?.let { builder.favicon(it) }
        }

        // player list (hover text)
        if (!isLocalPing) {
            val players = event.ping.players.getOrNull()
            val playerCountHandler = proxyPlugin.playerCountHandler
            val onlinePlayers = playerCountHandler.onlinePlayers(plugin.proxyServer.allPlayers.size)
            val realMax = playerCountHandler.maxPlayers(plugin.proxyServer.configuration.showMaxPlayers)

            val samplePlayers: List<SamplePlayer> =
                if (layout.playerList.enabled && layout.playerList.entries.isNotEmpty()) {
                    layout.playerList.entries.map { SamplePlayer(it, UUID.randomUUID()) }
                } else {
                    players?.sample ?: emptyList()
                }

            // slots
            val maxPlayers = if (layout.versionSettings.slots.enabled) {
                when (layout.versionSettings.slots.type) {
                    MaxPlayerDisplayType.REAL -> realMax
                    MaxPlayerDisplayType.FAKE -> layout.versionSettings.slots.fakeSlots
                    MaxPlayerDisplayType.DYNAMIC -> onlinePlayers + layout.versionSettings.slots.dynamicPlayerRange
                }
            } else {
                realMax
            }

            builder
                .onlinePlayers(onlinePlayers)
                .maximumPlayers(maxPlayers)
                .samplePlayers(*samplePlayers.toTypedArray())

            // version name
            if (layout.versionSettings.name.enabled) {
                builder.version(ServerPing.Version(event.ping.version.protocol, layout.versionSettings.name.text))
            }
        }

        event.ping = builder.build()
    }
}
