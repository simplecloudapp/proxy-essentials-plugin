package app.simplecloud.plugin.proxy.bungeecord.listener

import app.simplecloud.plugin.proxy.bungeecord.ProxyBungeeCordPlugin
import app.simplecloud.plugin.proxy.shared.config.motd.MaxPlayerDisplayType
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer
import net.md_5.bungee.api.Favicon
import net.md_5.bungee.api.ServerPing.*
import net.md_5.bungee.api.event.ProxyPingEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import java.awt.image.BufferedImage
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.*
import javax.imageio.ImageIO

class ProxyPingListener(
    private val plugin: ProxyBungeeCordPlugin
) : Listener {

    @EventHandler
    fun onPing(event: ProxyPingEvent) {
        runBlocking {

            val response = event.response

            val motdConfiguration = plugin.proxyPlugin.motdLayoutHandler.getCurrentMotdLayout()

            val firstLine = motdConfiguration.firstLines.random()
            val secondLine = motdConfiguration.secondLines.random()

            val messageOfTheDay = plugin.deserializeToComponent("$firstLine\n$secondLine")

            response.descriptionComponent = BungeeComponentSerializer.get().serialize(messageOfTheDay)[0]

            val socketAddress = event.connection.socketAddress as? InetSocketAddress
            val hostStringFromConnection = socketAddress?.address?.hostName ?: ""
            val hostStringFromServer = InetAddress.getLocalHost().hostAddress

            if (hostStringFromConnection != hostStringFromServer) {
                val playerList = motdConfiguration.playerInfo.map { PlayerInfo(it, UUID.randomUUID()) }
                response.players = when (motdConfiguration.maxPlayerDisplayType) {
                    null -> Players(
                        response.players.max,
                        response.players.online,
                        playerList.toTypedArray().ifEmpty { response.players.sample }
                    )

                    MaxPlayerDisplayType.REAL -> Players(
                        response.players.max,
                        response.players.online,
                        playerList.toTypedArray().ifEmpty { response.players.sample }
                    )

                    else -> Players(
                        response.players.online + motdConfiguration.dynamicPlayerRange,
                        response.players.online,
                        playerList.toTypedArray().ifEmpty { response.players.sample }
                    )
                }

                response.version = when (motdConfiguration.versionName) {
                    "" -> response.version
                    else -> Protocol(
                        motdConfiguration.versionName,
                        -1
                    )
                }
            }

            /*val favicon = if (motdConfiguration.serverIcon == "") {
                response.faviconObject
            } else {
                val serverIcon: BufferedImage = ImageIO.read(File(motdConfiguration.serverIcon))
                Favicon.create(serverIcon)
            }*/

            response.setFavicon(response.faviconObject)
        }
    }

}