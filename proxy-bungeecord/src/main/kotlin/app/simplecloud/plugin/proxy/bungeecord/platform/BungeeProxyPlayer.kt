package app.simplecloud.plugin.proxy.bungeecord.platform

import app.simplecloud.plugin.api.bungeecord.text.BungeeComponentConverter
import app.simplecloud.plugin.proxy.shared.platform.ProxyPlayer
import net.kyori.adventure.text.Component
import net.md_5.bungee.api.connection.ProxiedPlayer
import java.util.UUID

class BungeeProxyPlayer(
    private val player: ProxiedPlayer
) : ProxyPlayer {

    override fun getName(): String = player.name

    override fun getUniqueId(): UUID = player.uniqueId

    override fun hasPermission(permission: String) = player.hasPermission(permission)

    override fun getServerName(): String? = player.server?.info?.name

    override fun getPing() = player.ping.toLong()

    override fun sendPlayerListHeaderAndFooter(header: Component, footer: Component) {
        player.setTabHeader(BungeeComponentConverter.toBungee(header), BungeeComponentConverter.toBungee(footer))
    }
}
