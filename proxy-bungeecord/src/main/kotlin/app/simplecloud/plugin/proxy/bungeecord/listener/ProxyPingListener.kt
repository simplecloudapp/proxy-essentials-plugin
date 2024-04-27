package app.simplecloud.plugin.proxy.bungeecord.listener

import app.simplecloud.plugin.proxy.bungeecord.ProxyBungeeCordPlugin
import app.simplecloud.plugin.proxy.shared.config.motd.MaxPlayerDisplayType
import app.simplecloud.plugin.proxy.shared.event.MotdConfiguration
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer
import net.md_5.bungee.api.ServerPing.*
import net.md_5.bungee.api.event.ProxyPingEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.*

class ProxyPingListener(
    private val plugin: ProxyBungeeCordPlugin
) : Listener {

    @EventHandler
    fun onPing(event: ProxyPingEvent) {
        val socketAddress = event.connection.socketAddress as? InetSocketAddress
        val hostStringFromConnection = socketAddress?.address?.hostName?: ""
        val hostStringFromServer = InetAddress.getLocalHost().hostAddress

        if (hostStringFromConnection == hostStringFromServer) {
            return
        }

        val response = event.response

        val motdConfiguration = this.plugin.motdConfiguration

        val firstLine = motdConfiguration.firstLines.random()
        val secondLine = motdConfiguration.secondLines.random()

        val messageOfTheDay: Component = this.plugin.deserializeToComponent(firstLine + "\n" + secondLine)

        response.descriptionComponent = BungeeComponentSerializer.get().serialize(messageOfTheDay)[0]

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

}