package app.simplecloud.plugin.proxy.shared.platform

import net.kyori.adventure.text.Component
import java.util.UUID

interface ProxyPlayer {

    fun getName(): String

    fun getUniqueId(): UUID

    fun hasPermission(permission: String): Boolean

    fun getServerName(): String?

    fun getPing(): Long

    fun sendPlayerListHeaderAndFooter(header: Component, footer: Component)
}
