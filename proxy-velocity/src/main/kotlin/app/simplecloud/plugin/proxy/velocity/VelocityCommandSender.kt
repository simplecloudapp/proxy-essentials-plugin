package app.simplecloud.plugin.proxy.velocity

import app.simplecloud.plugin.proxy.shared.handler.command.CommandSender
import com.velocitypowered.api.command.CommandSource

class VelocityCommandSender(
    private val commandSource: CommandSource,
    val proxyVelocityPlugin: ProxyVelocityPlugin
) : CommandSender {

    fun getCommandSource(): CommandSource {
        return commandSource
    }

    override fun sendMessage(message: String) {
        commandSource.sendMessage(this.proxyVelocityPlugin.deserializeToComponent(message))
    }
}