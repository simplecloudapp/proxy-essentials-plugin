package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.plugin.proxy.velocity.ProxyVelocityPlugin
import app.simplecloud.plugin.proxy.velocity.event.ProxyPingConfigurationEvent
import com.velocitypowered.api.event.Subscribe
import net.kyori.adventure.text.TextReplacementConfig

class ProxyPingConfigurationListener(
    private val plugin: ProxyVelocityPlugin
) {

    @Subscribe
    fun onProxyPingConfiguration(proxyPingConfigurationEvent: ProxyPingConfigurationEvent) {
        var motd = proxyPingConfigurationEvent.messageOfTheDay

        val showMaxPlayers = this.plugin.proxyServer.configuration.showMaxPlayers
        val onlinePlayersCurrentProxy = this.plugin.proxyServer.allPlayers.size

        motd = motd.replaceText(TextReplacementConfig.builder().match("%ONLINE_PLAYERS%").replacement("soon").build())
        motd = motd.replaceText(TextReplacementConfig.builder().match("%ONLINE_PLAYERS_CURRENT_PROXY%").replacement("$onlinePlayersCurrentProxy").build())
        motd = motd.replaceText(TextReplacementConfig.builder().match("%MAX_PLAYERS%").replacement("$showMaxPlayers").build())
        motd = motd.replaceText(TextReplacementConfig.builder().match("%PROXY%").replacement("soon").build())

        proxyPingConfigurationEvent.messageOfTheDay = motd
    }
}