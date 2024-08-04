package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.plugin.proxy.shared.config.motd.MaxPlayerDisplayType
import app.simplecloud.plugin.proxy.velocity.ProxyVelocityPlugin
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.proxy.server.ServerPing
import com.velocitypowered.api.proxy.server.ServerPing.SamplePlayer
import com.velocitypowered.api.util.Favicon
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.InetAddress
import java.util.*
import javax.imageio.ImageIO
import kotlin.jvm.optionals.getOrNull


class ProxyPingListener(
    private val plugin: ProxyVelocityPlugin
) {

    @Subscribe
    fun onProxyPing(event: ProxyPingEvent) {
        val hostStringFromConnection = event.connection.remoteAddress.address.hostAddress
        val hostStringFromServer = InetAddress.getLocalHost().hostAddress

        if (hostStringFromConnection == hostStringFromServer) {
            return
        }

        val serverPing = event.ping

        val motdConfiguration = this.plugin.motdLayoutHandler.getCurrentMotdLayout()

        val firstLine = motdConfiguration.firstLines.random()
        val secondLine = motdConfiguration.secondLines.random()

        val messageOfTheDay = this.plugin.deserializeToComponent("$firstLine\n$secondLine")

        val playerList = motdConfiguration.playerInfo.map { SamplePlayer(it, UUID.randomUUID()) }

        val players = serverPing.players.getOrNull()
        val onlinePlayers = players?.online ?: 0
        val realMaxPlayers = players?.max ?: 0
        val maxPlayers = when (motdConfiguration.maxPlayerDisplayType) {
            MaxPlayerDisplayType.REAL -> realMaxPlayers
            MaxPlayerDisplayType.DYNAMIC -> onlinePlayers + motdConfiguration.dynamicPlayerRange
            null -> realMaxPlayers
        }

        val samplePlayers = playerList.ifEmpty { players?.sample ?: emptyList() }

        val versions: ServerPing.Version = when (motdConfiguration.versionName) {
            "" -> serverPing.version
            else -> ServerPing.Version(
                -1,
                motdConfiguration.versionName
            )
        }

        val favicon = if (motdConfiguration.serverIcon == "") {
            serverPing.favicon.orElse(null)
        } else {
            val serverIcon: BufferedImage = ImageIO.read(File(motdConfiguration.serverIcon))
            Favicon.create(serverIcon)
        }

        event.ping = event.ping.asBuilder()
            .version(versions)
            .onlinePlayers(onlinePlayers)
            .maximumPlayers(maxPlayers)
            .samplePlayers(*samplePlayers.toTypedArray())
            .description(messageOfTheDay)
            .favicon(favicon)
            .build()
    }

}