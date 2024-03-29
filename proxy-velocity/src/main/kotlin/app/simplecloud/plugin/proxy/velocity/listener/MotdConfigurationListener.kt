package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.plugin.proxy.velocity.ProxyVelocityPlugin
import app.simplecloud.plugin.proxy.velocity.event.MotdConfigurationEvent
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import net.kyori.adventure.text.TextReplacementConfig

class MotdConfigurationListener(
    private val plugin: ProxyVelocityPlugin
) {

    @Subscribe(order = PostOrder.LAST)
    fun onMotdConfiguration(event: MotdConfigurationEvent) {
        val pingConfiguration = event.pingConfiguration
        var motd = pingConfiguration.messageOfTheDay

        val showMaxPlayers = this.plugin.proxyServer.configuration.showMaxPlayers
        val onlinePlayers = this.plugin.proxyServer.allPlayers.size

        motd = motd.replaceText(TextReplacementConfig.builder().match("%ONLINE_PLAYERS%").replacement("$onlinePlayers").build())
        motd = motd.replaceText(TextReplacementConfig.builder().match("%MAX_PLAYERS%").replacement("$showMaxPlayers").build())

        pingConfiguration.messageOfTheDay = motd

        val playerInfo = pingConfiguration.playerInfo
        pingConfiguration.playerInfo = playerInfo.map { replace(it) }

        pingConfiguration.versionName = replace(pingConfiguration.versionName)

        event.pingConfiguration = pingConfiguration
    }

    private fun replace(content: String): String {
        return content.replace("%ONLINE_PLAYERS%", this.plugin.proxyServer.allPlayers.size.toString())
            .replace("%MAX_PLAYERS%", this.plugin.proxyServer.configuration.showMaxPlayers.toString())
    }
}