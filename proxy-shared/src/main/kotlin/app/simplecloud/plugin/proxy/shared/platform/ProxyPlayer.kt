package app.simplecloud.plugin.proxy.shared.platform

import net.kyori.adventure.text.Component

interface ProxyPlayer {

    fun getServerName(): String?

    fun getPing(): Long

    fun sendPlayerListHeaderAndFooter(header: Component, footer: Component)
}
