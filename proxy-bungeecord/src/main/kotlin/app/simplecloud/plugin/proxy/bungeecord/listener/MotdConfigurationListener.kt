package app.simplecloud.plugin.proxy.bungeecord.listener

import app.simplecloud.plugin.proxy.bungeecord.ProxyBungeeCordPlugin
import app.simplecloud.plugin.proxy.bungeecord.event.MotdConfigurationEvent
import net.kyori.adventure.text.TextReplacementConfig
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler

class MotdConfigurationListener(
    private val plugin: ProxyBungeeCordPlugin
): Listener {

    @EventHandler
    fun onMotdConfiguration(event: MotdConfigurationEvent) {
        val pingConfiguration = event.pingConfiguration
        var motd = pingConfiguration.messageOfTheDay

        val showMaxPlayers = this.plugin.proxy.config.playerLimit
        val onlinePlayers = this.plugin.proxy.players.size

        motd = motd.replaceText(TextReplacementConfig.builder().match("%ONLINE_PLAYERS%").replacement("$onlinePlayers").build())
        motd = motd.replaceText(TextReplacementConfig.builder().match("%MAX_PLAYERS%").replacement("$showMaxPlayers").build())

        pingConfiguration.messageOfTheDay = motd

        val playerInfo = pingConfiguration.playerInfo
        pingConfiguration.playerInfo = playerInfo.map { replace(it) }

        pingConfiguration.versionName = replace(pingConfiguration.versionName)

        event.pingConfiguration = pingConfiguration
    }

    private fun replace(content: String): String {
        return content.replace("%ONLINE_PLAYERS%", this.plugin.proxy.players.size.toString())
            .replace("%MAX_PLAYERS%", this.plugin.proxy.config.playerLimit.toString())
    }
}