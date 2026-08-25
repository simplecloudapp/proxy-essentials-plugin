package app.simplecloud.plugin.proxy.bungeecord.listener

import app.simplecloud.plugin.proxy.bungeecord.ProxyBungeeCordPlugin
import app.simplecloud.plugin.proxy.shared.config.MaxPlayerDisplayType
import app.simplecloud.plugin.proxy.shared.utilities.LocalPingSourceMatcher
import app.simplecloud.plugin.proxy.shared.utilities.ServerIconLoader
import net.md_5.bungee.api.Favicon
import net.md_5.bungee.api.ServerPing.*
import net.md_5.bungee.api.event.ProxyPingEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import java.net.InetSocketAddress
import java.nio.file.Path
import java.util.*

class ProxyPingListener(
    private val plugin: ProxyBungeeCordPlugin
) : Listener {

    private val serverIconLoader = ServerIconLoader(
        Path.of(plugin.proxyPlugin.serverIconsPath)
    ) { image -> Favicon.create(image) }
    private val localPingSourceMatcher = LocalPingSourceMatcher()

    @EventHandler
    fun onPing(event: ProxyPingEvent) {
        val virtualHost = event.connection.virtualHost?.hostName
        val layout = virtualHost
            ?.let { plugin.proxyPlugin.domainMotdHandler.getLayoutNameForDomain(it) }
            ?.let { plugin.proxyPlugin.motdLayoutHandler.getLayoutByName(it) }
            ?: plugin.proxyPlugin.motdLayoutHandler.getCurrentMotdLayout()

        if (!layout.motd.enabled) return

        val entry = plugin.proxyPlugin.motdLayoutHandler.selectEntry(layout, layout.configVersion)
            ?: return

        val response = event.response
        val motd = plugin.deserializeMotd(entry.line1, entry.line2)
        response.descriptionComponent = net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer.get().serialize(motd)[0]

        val socketAddress = event.connection.socketAddress as? InetSocketAddress
        val isLocalPing = localPingSourceMatcher.isLocal(socketAddress?.address)

        // server icon
        if (layout.serverIcon.enabled) {
            serverIconLoader.get(layout.serverIcon.file) { plugin.logger.warning(it) }
                ?.let { response.setFavicon(it) }
        }

        if (!isLocalPing) {
            // player list (hover text)
            val samplePlayers = if (layout.playerList.enabled && layout.playerList.playerList.isNotEmpty()) {
                layout.playerList.playerList.map { PlayerInfo(it, UUID.randomUUID()) }.toTypedArray()
            } else {
                response.players.sample
            }

            // slots
            val playerCountHandler = plugin.proxyPlugin.playerCountHandler
            val onlinePlayers = playerCountHandler.onlinePlayers(plugin.proxy.players.size)
            val realMax = playerCountHandler.maxPlayers(plugin.proxy.config.playerLimit)
            val maxPlayers = if (layout.version.slots.enabled) {
                when (layout.version.slots.type) {
                    MaxPlayerDisplayType.REAL -> realMax
                    MaxPlayerDisplayType.FAKE -> layout.version.slots.fakeSlots
                    MaxPlayerDisplayType.DYNAMIC -> onlinePlayers + layout.version.slots.dynamicPlayerRange
                }
            } else {
                realMax
            }

            response.players = Players(maxPlayers, onlinePlayers, samplePlayers)

            // version name
            if (layout.version.name.enabled) {
                response.version = Protocol(layout.version.name.text, response.version.protocol)
            }
        }
    }
}
