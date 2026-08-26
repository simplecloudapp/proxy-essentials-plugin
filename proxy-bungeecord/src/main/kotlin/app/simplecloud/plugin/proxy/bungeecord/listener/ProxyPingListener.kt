package app.simplecloud.plugin.proxy.bungeecord.listener

import app.simplecloud.plugin.proxy.bungeecord.ProxyBungeeCordPlugin
import app.simplecloud.plugin.proxy.bungeecord.toBaseComponent
import app.simplecloud.plugin.proxy.shared.config.MotdLayoutConfiguration
import app.simplecloud.plugin.proxy.shared.motd.ServerIconLoader
import app.simplecloud.plugin.proxy.shared.utilities.LocalPingSourceMatcher
import net.md_5.bungee.api.Favicon
import net.md_5.bungee.api.ServerPing
import net.md_5.bungee.api.event.ProxyPingEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import java.net.InetSocketAddress
import java.nio.file.Path
import java.util.UUID

class ProxyPingListener(
    private val plugin: ProxyBungeeCordPlugin
) : Listener {

    private val proxyPlugin = plugin.proxyPlugin

    private val serverIconLoader = ServerIconLoader(Path.of(proxyPlugin.serverIconsPath)) { image ->
        Favicon.create(image)
    }

    @EventHandler
    fun onPing(event: ProxyPingEvent) {
        val layout = resolveLayout(event.connection.virtualHost?.hostName)
        if (!layout.motd.enabled) return

        val entry = proxyPlugin.layoutRepository.selectEntry(layout, layout.configVersion) ?: return
        val response = event.response
        response.descriptionComponent = plugin.deserializeMotd(entry.line1, entry.line2).toBaseComponent()

        applyServerIcon(response, layout)

        // The cloud pings the proxy itself to check its health, which must not affect the counts.
        val socketAddress = event.connection.socketAddress as? InetSocketAddress
        if (!LocalPingSourceMatcher.isLocal(socketAddress?.address)) {
            applyPlayerList(response, layout)
        }
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

    private fun applyServerIcon(response: ServerPing, layout: MotdLayoutConfiguration) {
        if (!layout.serverIcon.enabled) return

        val favicon = serverIconLoader.get(layout.serverIcon.file) { plugin.logger.warning(it) } ?: return
        response.setFavicon(favicon)
    }

    private fun applyPlayerList(response: ServerPing, layout: MotdLayoutConfiguration) {
        val playerCountHandler = proxyPlugin.playerCountTracker
        val onlinePlayers = playerCountHandler.onlinePlayers(plugin.proxy.players.size)
        val realMaxPlayers = playerCountHandler.maxPlayers(plugin.proxy.config.playerLimit)
        val maxPlayers = layout.version.slots.resolveMaxPlayers(onlinePlayers, realMaxPlayers)

        response.players = ServerPing.Players(maxPlayers, onlinePlayers, samplePlayers(layout, response))

        if (layout.version.name.enabled) {
            response.version = ServerPing.Protocol(layout.version.name.text, response.version.protocol)
        }
    }

    private fun samplePlayers(layout: MotdLayoutConfiguration, response: ServerPing): Array<ServerPing.PlayerInfo> {
        if (!layout.playerList.enabled || layout.playerList.playerList.isEmpty()) {
            return response.players.sample
        }

        return layout.playerList.playerList
            .map { ServerPing.PlayerInfo(it, UUID.randomUUID()) }
            .toTypedArray()
    }
}