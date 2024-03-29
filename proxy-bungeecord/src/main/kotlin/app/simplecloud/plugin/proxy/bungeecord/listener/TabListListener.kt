package app.simplecloud.plugin.proxy.bungeecord.listener

import app.simplecloud.plugin.proxy.bungeecord.ProxyBungeeCordPlugin
import app.simplecloud.plugin.proxy.bungeecord.event.TabListConfigurationEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.minimessage.MiniMessage
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.event.ServerConnectedEvent
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import java.text.SimpleDateFormat
import java.util.*

class TabListListener(
    private val plugin: ProxyBungeeCordPlugin
): Listener {

    private val miniMessage = MiniMessage.miniMessage()

    @EventHandler
    fun onTabListConfiguration(event: TabListConfigurationEvent) {
        var header = event.tabListConfiguration.header
        header = this.replaceText(header, event.player)
        event.tabListConfiguration.header = header

        var footer = event.tabListConfiguration.footer
        footer = this.replaceText(footer, event.player)
        event.tabListConfiguration.footer = footer
    }

    @EventHandler
    fun onServerConnected(event: ServerConnectedEvent) {
        val player = event.player
        this.plugin.tabListHandler.updateTabListForPlayer(player)
    }

    private fun replaceText(component: Component, player: ProxiedPlayer): Component {
        val placeHolderConfiguration = this.plugin.placeHolderConfiguration

        val showMaxPlayers = this.plugin.proxy.config.playerLimit
        val onlinePlayers = this.plugin.proxy.players.size
        val currentTime = SimpleDateFormat(placeHolderConfiguration.currentTimeFormat).format(Calendar.getInstance().time)
        val currentDate = SimpleDateFormat(placeHolderConfiguration.currentDateFormat).format(Calendar.getInstance().time)

        val service = player.server.info.name

        val ping = player.ping
        val pingColors = placeHolderConfiguration.pingColors
        val pingColor = pingColors.firstOrNull { it.ping >= ping }?.color ?: "<dark_red>"

        var replacedComponent = component

        replacedComponent = replacedComponent.replaceText(
            TextReplacementConfig.builder().match("%ONLINE_PLAYERS%").replacement(
            "$onlinePlayers"
        ).build())
        replacedComponent = replacedComponent.replaceText(
            TextReplacementConfig.builder().match("%MAX_PLAYERS%").replacement(
            "$showMaxPlayers"
        ).build())
        replacedComponent = replacedComponent.replaceText(
            TextReplacementConfig.builder().match("%SERVICE_NAME%").replacement(
            service
        ).build())
        replacedComponent = replacedComponent.replaceText(
            TextReplacementConfig.builder().match("%CURRENT_TIME%").replacement(
            currentTime
        ).build())
        replacedComponent = replacedComponent.replaceText(
            TextReplacementConfig.builder().match("%CURRENT_DATE%").replacement(
            currentDate
        ).build())
        replacedComponent = replacedComponent.replaceText(
            TextReplacementConfig.builder().match("%PING%").replacement(
            this.miniMessage.deserialize(pingColor + ping)
        ).build())

        return replacedComponent
    }
}