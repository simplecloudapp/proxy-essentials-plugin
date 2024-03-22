package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.plugin.proxy.velocity.ProxyVelocityPlugin
import app.simplecloud.plugin.proxy.velocity.event.ProxyPingConfigurationEvent
import com.velocitypowered.api.event.Subscribe
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.minimessage.MiniMessage

class ProxyPingConfigurationListener(
    private val plugin: ProxyVelocityPlugin
) {

    private val miniMessage = MiniMessage.miniMessage()

    @Subscribe
    fun onProxyPingConfiguration(proxyPingConfigurationEvent: ProxyPingConfigurationEvent) {
        var messageOfTheDay = proxyPingConfigurationEvent.messageOfTheDay
        messageOfTheDay = messageOfTheDay.replaceText(TextReplacementConfig.builder().match("%PROXY%").replacement("TEST").build())
        proxyPingConfigurationEvent.messageOfTheDay = messageOfTheDay
    }
}