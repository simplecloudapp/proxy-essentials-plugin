package app.simplecloud.plugin.proxy.bungeecord.listener

import app.simplecloud.plugin.proxy.shared.ProxyEssentials
import app.simplecloud.plugin.proxy.shared.layout.ServerIconCache
import app.simplecloud.plugin.proxy.shared.utilities.LocalPingSourceMatcher
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer
import net.md_5.bungee.api.Favicon
import net.md_5.bungee.api.ServerPing
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.event.ProxyPingEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import java.io.ByteArrayInputStream
import java.net.InetSocketAddress
import java.util.Base64
import java.util.UUID
import javax.imageio.ImageIO

class ProxyPingListener(
    private val essentials: ProxyEssentials
) : Listener {

    private val serverIconCache = ServerIconCache(essentials.serverIconDirectory)

    @EventHandler
    fun onProxyPing(event: ProxyPingEvent) {
        val layoutService = essentials.layoutService
        val layoutName = layoutService.getLayoutName(event.connection.virtualHost?.hostString)
        val layout = layoutService.getLayout(layoutName)
        val response = event.response

        val motdEntry = if (layout.motd.enabled) layoutService.getMotdEntry(layoutName, layout) else null
        if (motdEntry != null) {
            val motd = essentials.messageFormatter.formatMotd(motdEntry.line1, motdEntry.line2)
            response.descriptionComponent = TextComponent(*BungeeComponentSerializer.get().serialize(motd))
        }

        val icon = if (layout.serverIcon.enabled) serverIconCache.get(layout.serverIcon.file) else null
        if (icon != null) {
            val bytes = Base64.getDecoder().decode(icon.substringAfter(","))
            response.setFavicon(Favicon.create(ImageIO.read(ByteArrayInputStream(bytes))))
        }

        if (layout.version.name.enabled) {
            response.version = ServerPing.Protocol(layout.version.name.text, response.version.protocol)
        }

        val address = event.connection.socketAddress as? InetSocketAddress
        if (!LocalPingSourceMatcher.isLocal(address?.address)) {
            val onlinePlayers = essentials.playerCountService.getOnlinePlayers()
            val maxPlayers = layout.version.slots.resolveMaxPlayers(onlinePlayers, essentials.playerCountService.getMaxPlayers())
            val samplePlayers = if (layout.playerList.enabled && layout.playerList.playerList.isNotEmpty()) {
                layout.playerList.playerList.map { ServerPing.PlayerInfo(it, UUID.randomUUID()) }.toTypedArray()
            } else {
                response.players.sample
            }
            response.players = ServerPing.Players(maxPlayers, onlinePlayers, samplePlayers)
        }
    }
}
