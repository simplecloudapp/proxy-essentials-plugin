package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.plugin.proxy.shared.ProxyEssentials
import app.simplecloud.plugin.proxy.shared.layout.ServerIconCache
import app.simplecloud.plugin.proxy.shared.utilities.LocalPingSourceMatcher
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.proxy.server.ServerPing
import com.velocitypowered.api.util.Favicon
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

class ProxyPingListener(
    private val essentials: ProxyEssentials
) {

    private val serverIconCache = ServerIconCache(essentials.serverIconDirectory)

    @Subscribe
    fun onProxyPing(event: ProxyPingEvent) {
        val layoutService = essentials.layoutService
        val layoutName = layoutService.getLayoutName(event.connection.virtualHost.getOrNull()?.hostString)
        val layout = layoutService.getLayout(layoutName)
        val builder = event.ping.asBuilder()

        val motdEntry = if (layout.motd.enabled) layoutService.getMotdEntry(layoutName, layout) else null
        if (motdEntry != null) {
            builder.description(essentials.messageFormatter.formatMotd(motdEntry.line1, motdEntry.line2))
        }

        val icon = if (layout.serverIcon.enabled) serverIconCache.get(layout.serverIcon.file) else null
        if (icon != null) {
            builder.favicon(Favicon(icon))
        }

        if (layout.version.name.enabled) {
            builder.version(ServerPing.Version(event.ping.version.protocol, layout.version.name.text))
        }

        if (!LocalPingSourceMatcher.isLocal(event.connection.remoteAddress.address)) {
            val onlinePlayers = essentials.playerCountService.getOnlinePlayers()
            val maxPlayers = layout.version.slots.resolveMaxPlayers(onlinePlayers, essentials.playerCountService.getMaxPlayers())
            builder.onlinePlayers(onlinePlayers).maximumPlayers(maxPlayers)

            if (layout.playerList.enabled && layout.playerList.playerList.isNotEmpty()) {
                builder.clearSamplePlayers()
                builder.samplePlayers(*layout.playerList.playerList.map { ServerPing.SamplePlayer(it, UUID.randomUUID()) }.toTypedArray())
            }
        }

        event.ping = builder.build()
    }
}
