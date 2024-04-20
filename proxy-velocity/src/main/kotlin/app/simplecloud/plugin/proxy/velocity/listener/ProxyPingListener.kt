package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.plugin.proxy.shared.config.motd.MaxPlayerDisplayType
import app.simplecloud.plugin.proxy.shared.event.MotdConfiguration
import app.simplecloud.plugin.proxy.velocity.ProxyVelocityPlugin
import app.simplecloud.plugin.proxy.velocity.event.MotdConfigurationEvent
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.proxy.server.ServerPing
import com.velocitypowered.api.proxy.server.ServerPing.SamplePlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import java.net.InetAddress
import java.util.*
import kotlin.jvm.optionals.getOrNull

class ProxyPingListener(
    private val plugin: ProxyVelocityPlugin
) {

    private val miniMessage = MiniMessage.miniMessage()

    @Subscribe
    fun onProxyPing(event: ProxyPingEvent) {
        val hostStringFromConnection = event.connection.remoteAddress.address.hostAddress
        val hostStringFromServer = InetAddress.getLocalHost().hostAddress

        if (hostStringFromConnection == hostStringFromServer) {
            return
        }

        val serverPing = event.ping

        val motdConfiguration = this.plugin.motdConfiguration

        val firstLine = motdConfiguration.firstLines.random()
        val secondLine = motdConfiguration.secondLines.random()

        val messageOfTheDay: Component = this.miniMessage.deserialize(firstLine + "\n" + secondLine)

        var proxyPingConfigurationEvent = MotdConfigurationEvent(
            MotdConfiguration(
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

        val players = serverPing.players.getOrNull()
        val onlinePlayers = players?.online?: 0
        val realMaxPlayers = players?.max?: 0
        val maxPlayers = when(pingConfiguration.maxPlayerDisplayType) {
            MaxPlayerDisplayType.REAL -> realMaxPlayers
            MaxPlayerDisplayType.DYNAMIC -> onlinePlayers + pingConfiguration.dynamicPlayerRange
            null -> realMaxPlayers
        }

        val samplePlayers = playerList.ifEmpty { players?.sample?: emptyList() }

        val versions: ServerPing.Version = when(motdConfiguration.versionName) {
            "" -> serverPing.version
            else -> ServerPing.Version(
                -1,
                motdConfiguration.versionName
            )
        }

        event.ping = event.ping.asBuilder()
            .version(versions)
            .onlinePlayers(onlinePlayers)
            .maximumPlayers(maxPlayers)
            .samplePlayers(*samplePlayers.toTypedArray())
            .description(pingConfiguration.messageOfTheDay)
            .build()
    }
}