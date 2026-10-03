package app.simplecloud.plugin.proxy.bungeecord.platform

import app.simplecloud.plugin.proxy.shared.platform.ProxyPlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer
import net.md_5.bungee.api.connection.ProxiedPlayer

class BungeeProxyPlayer(
    private val player: ProxiedPlayer
) : ProxyPlayer {

    override fun getServerName(): String? = player.server?.info?.name

    override fun getPing() = player.ping.toLong()

    override fun sendPlayerListHeaderAndFooter(header: Component, footer: Component) {
        val serializer = BungeeComponentSerializer.get()
        player.setTabHeader(serializer.serialize(header), serializer.serialize(footer))
    }
}
