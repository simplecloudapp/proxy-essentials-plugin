package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.plugin.proxy.velocity.ProxyVelocityPlugin
import app.simplecloud.plugin.proxy.velocity.event.TabListConfigurationEvent
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.ServerPostConnectEvent
import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.minimessage.MiniMessage
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlin.jvm.optionals.getOrNull

class TabListListener(
    private val plugin: ProxyVelocityPlugin
) {

    private val miniMessage = MiniMessage.miniMessage()

    @Subscribe(order = PostOrder.LAST)
    fun onTabListConfiguration(event: TabListConfigurationEvent) {
        var header = event.tabListConfiguration.header
        header = this.replaceText(header, event.player)
        event.tabListConfiguration.header = header

        var footer = event.tabListConfiguration.footer
        footer = this.replaceText(footer, event.player)
        event.tabListConfiguration.footer = footer
    }

    @Subscribe(order = PostOrder.LAST)
    fun onServerConnected(event: ServerPostConnectEvent) {
        val player = event.player
        this.plugin.tabListHandler.updateTabListForPlayer(player)
    }

    private fun replaceText(component: Component, player: Player): Component {
        val placeHolderConfiguration = this.plugin.placeHolderConfiguration

        val showMaxPlayers = this.plugin.proxyServer.configuration.showMaxPlayers
        val onlinePlayers = this.plugin.proxyServer.allPlayers.size
        val currentTime = SimpleDateFormat(placeHolderConfiguration.currentTimeFormat).format(Calendar.getInstance().time)
        val currentDate = SimpleDateFormat(placeHolderConfiguration.currentDateFormat).format(Calendar.getInstance().time)

        val serverConnection = player.currentServer.getOrNull()
        val service = serverConnection?.serverInfo?.name ?: "Unknown"

        val ping = player.ping
        val pingColors = placeHolderConfiguration.pingColors
        val pingColor = pingColors.firstOrNull { it.ping >= ping }?.color ?: "<dark_red>"

        var replacedComponent = component

        replacedComponent = replacedComponent.replaceText(TextReplacementConfig.builder().match("%ONLINE_PLAYERS%").replacement(
            "$onlinePlayers"
        ).build())
        replacedComponent = replacedComponent.replaceText(TextReplacementConfig.builder().match("%MAX_PLAYERS%").replacement(
            "$showMaxPlayers"
        ).build())
        replacedComponent = replacedComponent.replaceText(TextReplacementConfig.builder().match("%SERVICE_NAME%").replacement(
            service
        ).build())
        replacedComponent = replacedComponent.replaceText(TextReplacementConfig.builder().match("%CURRENT_TIME%").replacement(
            currentTime
        ).build())
        replacedComponent = replacedComponent.replaceText(TextReplacementConfig.builder().match("%CURRENT_DATE%").replacement(
            currentDate
        ).build())
        replacedComponent = replacedComponent.replaceText(TextReplacementConfig.builder().match("%PING%").replacement(
            this.miniMessage.deserialize(pingColor + ping)
        ).build())

        return replacedComponent
    }
}