package app.simplecloud.plugin.proxy.velocity.platform

import app.simplecloud.plugin.proxy.shared.platform.ProxyPlayer
import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.Component
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

class VelocityProxyPlayer(
    private val player: Player
) : ProxyPlayer {

    override fun getName(): String = player.username

    override fun getUniqueId(): UUID = player.uniqueId

    override fun hasPermission(permission: String) = player.hasPermission(permission)

    override fun getServerName() = player.currentServer.getOrNull()?.serverInfo?.name

    override fun getPing() = player.ping

    override fun sendPlayerListHeaderAndFooter(header: Component, footer: Component) {
        player.sendPlayerListHeaderAndFooter(header, footer)
    }
}
