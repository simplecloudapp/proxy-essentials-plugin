package app.simplecloud.plugin.proxy.shared.command

import net.kyori.adventure.text.Component

interface ProxySender {

    fun sendMessage(message: Component)
}
