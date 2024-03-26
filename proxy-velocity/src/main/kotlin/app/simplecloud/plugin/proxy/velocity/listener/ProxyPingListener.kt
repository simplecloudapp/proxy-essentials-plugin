package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.plugin.proxy.shared.config.motd.MaxPlayerDisplayType
import app.simplecloud.plugin.proxy.shared.event.ProxyPingConfiguration
import app.simplecloud.plugin.proxy.velocity.ProxyVelocityPlugin
import app.simplecloud.plugin.proxy.velocity.event.ProxyPingConfigurationEvent
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.proxy.server.ServerPing
import com.velocitypowered.api.proxy.server.ServerPing.SamplePlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import java.util.*

class ProxyPingListener(
    private val plugin: ProxyVelocityPlugin
) {

    private val miniMessage = MiniMessage.miniMessage()

    @Subscribe
    fun onProxyPing(event: ProxyPingEvent) {
        val serverPing = event.ping

        val motdConfiguration = this.plugin.motdConfiguration

        val firstLine = motdConfiguration.firstLines.random()
        val secondLine = motdConfiguration.secondLines.random()

        val messageOfTheDay: Component = this.miniMessage.deserialize(firstLine + "\n" + secondLine)

        var proxyPingConfigurationEvent = ProxyPingConfigurationEvent(
            ProxyPingConfiguration(
                messageOfTheDay,
                motdConfiguration.playerInfo,
                motdConfiguration.versionName,
                motdConfiguration.maxPlayerDisplayType,
                motdConfiguration.dynamicPlayerRange
            )
        )

        proxyPingConfigurationEvent = this.plugin.proxyServer.eventManager.fire(proxyPingConfigurationEvent).get()
        val pingConfiguration = proxyPingConfigurationEvent.pingConfiguration

        val playerList = pingConfiguration.playerInfo.map { SamplePlayer(it, UUID.randomUUID()) }
        val players: ServerPing.Players = when(pingConfiguration.maxPlayerDisplayType) {
            null -> ServerPing.Players(
                serverPing.players.get().online,
                serverPing.players.get().max,
                playerList.ifEmpty { serverPing.players.get().sample }
            )
            MaxPlayerDisplayType.REAL -> ServerPing.Players(
                serverPing.players.get().online,
                serverPing.players.get().max,
                playerList.ifEmpty { serverPing.players.get().sample }
            )
            else -> ServerPing.Players(
                serverPing.players.get().online,
                serverPing.players.get().online + pingConfiguration.dynamicPlayerRange,
                playerList.ifEmpty { serverPing.players.get().sample }
            )
        }

        val versions: ServerPing.Version = when(motdConfiguration.versionName) {
            "" -> serverPing.version
            else -> ServerPing.Version(
                serverPing.version.protocol,
                motdConfiguration.versionName
            )
        }

        val ping = ServerPing(
            versions,
            players,
            pingConfiguration.messageOfTheDay,
            serverPing.favicon.get()
        )

        event.ping = ping
    }
}