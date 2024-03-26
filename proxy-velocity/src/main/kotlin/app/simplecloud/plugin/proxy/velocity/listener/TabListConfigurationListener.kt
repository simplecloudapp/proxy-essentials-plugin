package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.plugin.proxy.velocity.ProxyVelocityPlugin
import app.simplecloud.plugin.proxy.velocity.event.TabListConfigurationEvent
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import kotlin.jvm.optionals.getOrNull

class TabListConfigurationListener(
    private val plugin: ProxyVelocityPlugin
) {

    @Subscribe(order = PostOrder.LAST)
    fun onTabListConfiguration(event: TabListConfigurationEvent) {
        var header = event.tabListConfiguration.header
        header = this.replaceText(header, event.player)
        event.tabListConfiguration.header = header

        var footer = event.tabListConfiguration.footer
        footer = this.replaceText(footer, event.player)
        event.tabListConfiguration.footer = footer
    }

    private fun replaceText(component: Component, player: Player): Component {
        val showMaxPlayers = this.plugin.proxyServer.configuration.showMaxPlayers
        val onlinePlayers = this.plugin.proxyServer.allPlayers.size

        val serverConnection = player.currentServer.getOrNull()
        val service = serverConnection?.serverInfo?.name ?: "Unknown"

        var replacedComponent = component

        replacedComponent = replacedComponent.replaceText(TextReplacementConfig.builder().match("%ONLINE_PLAYERS%").replacement("$onlinePlayers").build())
        replacedComponent = replacedComponent.replaceText(TextReplacementConfig.builder().match("%MAX_PLAYERS%").replacement("$showMaxPlayers").build())
        replacedComponent = replacedComponent.replaceText(TextReplacementConfig.builder().match("%SERVICE_NAME%").replacement(service).build())

        return replacedComponent
    }
}