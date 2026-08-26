package app.simplecloud.plugin.proxy.velocity

import app.simplecloud.plugin.proxy.shared.command.ProxyCommandSender
import com.velocitypowered.api.command.CommandSource

class VelocityCommandSender(val commandSource: CommandSource, private val plugin: ProxyVelocityPlugin) : ProxyCommandSender {

    override fun sendMessage(message: String) {
        commandSource.sendMessage(plugin.deserializeToComponent(message))
    }

}
