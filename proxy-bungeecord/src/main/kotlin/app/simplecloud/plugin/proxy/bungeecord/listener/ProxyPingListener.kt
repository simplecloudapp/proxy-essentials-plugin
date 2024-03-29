package app.simplecloud.plugin.proxy.bungeecord.listener

import app.simplecloud.plugin.proxy.bungeecord.ProxyBungeeCordPlugin
import app.simplecloud.plugin.proxy.bungeecord.event.MotdConfigurationEvent
import app.simplecloud.plugin.proxy.shared.config.motd.MaxPlayerDisplayType
import app.simplecloud.plugin.proxy.shared.event.MotdConfiguration
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.md_5.bungee.api.ServerPing.PlayerInfo
import net.md_5.bungee.api.ServerPing.Players
import net.md_5.bungee.api.ServerPing.Protocol
import net.md_5.bungee.api.event.ProxyPingEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import java.util.*

class ProxyPingListener(
    private val plugin: ProxyBungeeCordPlugin
): Listener {

    private val miniMessage = MiniMessage.miniMessage()


    @EventHandler
    fun onPing(event: ProxyPingEvent) {
        val response = event.response

        val motdConfiguration = this.plugin.motdConfiguration

        val firstLine = motdConfiguration.firstLines.random()
        val secondLine = motdConfiguration.secondLines.random()

        val messageOfTheDay: Component = this.miniMessage.deserialize(firstLine + "\n" + secondLine)

        val pingConfiguration = this.plugin.proxy.pluginManager.callEvent(
            MotdConfigurationEvent(
                MotdConfiguration(
                    messageOfTheDay,
                    motdConfiguration.playerInfo,
                    motdConfiguration.versionName,
                    motdConfiguration.maxPlayerDisplayType,
                    motdConfiguration.dynamicPlayerRange
                )
            )
        ).pingConfiguration

        response.descriptionComponent = BungeeComponentSerializer.get().serialize(pingConfiguration.messageOfTheDay)[0]

        val playerList = pingConfiguration.playerInfo.map { PlayerInfo(it, UUID.randomUUID()) }
        response.players =  when(pingConfiguration.maxPlayerDisplayType) {
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
                response.players.online + pingConfiguration.dynamicPlayerRange,
                response.players.online,
                playerList.toTypedArray().ifEmpty { response.players.sample }
            )
        }

        response.version = when(pingConfiguration.versionName) {
            "" -> response.version
            else -> Protocol(
                pingConfiguration.versionName,
                -1
            )
        }
    }
}