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
    fun onMotdConfiguration(motdConfigurationEvent: MotdConfigurationEvent) {
        var motd = motdConfigurationEvent.pingConfiguration.messageOfTheDay

        val showMaxPlayers = this.plugin.proxyServer.configuration.showMaxPlayers
        val onlinePlayers = this.plugin.proxyServer.allPlayers.size

        motd = motd.replaceText(TextReplacementConfig.builder().match("%ONLINE_PLAYERS%").replacement("$onlinePlayers").build())
        motd = motd.replaceText(TextReplacementConfig.builder().match("%MAX_PLAYERS%").replacement("$showMaxPlayers").build())

        motdConfigurationEvent.pingConfiguration.messageOfTheDay = motd
    }
}