package app.simplecloud.plugin.proxy.bungeecord.command

import app.simplecloud.plugin.proxy.shared.command.ProxySender
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer
import net.md_5.bungee.api.CommandSender

class BungeeCordCommandSender(
    val commandSender: CommandSender
) : ProxySender {

    override fun sendMessage(message: Component) {
        commandSender.sendMessage(*BungeeComponentSerializer.get().serialize(message))
    }

    override fun hasPermission(permission: String) = commandSender.hasPermission(permission)
}