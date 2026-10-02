package app.simplecloud.plugin.proxy.velocity.command

import app.simplecloud.plugin.proxy.shared.command.ProxySender
import com.velocitypowered.api.command.CommandSource
import net.kyori.adventure.text.Component

class VelocityCommandSender(
    val source: CommandSource
) : ProxySender {

    override fun sendMessage(message: Component) {
        source.sendMessage(message)
    }

    override fun hasPermission(permission: String) = source.hasPermission(permission)
}