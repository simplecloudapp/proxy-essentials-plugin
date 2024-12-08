package app.simplecloud.plugin.proxy.bungeecord

import app.simplecloud.plugin.proxy.shared.handler.command.CommandSender
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer
import net.md_5.bungee.api.chat.BaseComponent

class BungeeCordCommandSender(private val commandSender: net.md_5.bungee.api.CommandSender
) : CommandSender {

    fun getCommandSender(): net.md_5.bungee.api.CommandSender {
        return commandSender
    }

    override fun sendMessage(message: String) {
        commandSender.sendMessage(message)
    }
}

fun Component.toBaseComponent(): BaseComponent {
    return BungeeComponentSerializer.get().serialize(this)[0]
}