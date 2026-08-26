package app.simplecloud.plugin.proxy.bungeecord

import app.simplecloud.plugin.proxy.shared.command.ProxyCommandSender
import net.kyori.adventure.platform.bungeecord.BungeeAudiences
import net.kyori.adventure.text.minimessage.MiniMessage
import net.md_5.bungee.api.CommandSender

class BungeeCordCommandSender(
    val commandSender: CommandSender,
    private val adventure: BungeeAudiences?
) : ProxyCommandSender {

    override fun sendMessage(message: String) {
        adventure?.sender(commandSender)?.sendMessage(MiniMessage.miniMessage().deserialize(message))
    }
}
