package app.simplecloud.plugin.proxy.bungeecord

import app.simplecloud.plugin.proxy.shared.handler.command.CommandSender
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.connection.ProxiedPlayer

class BungeeCordCommandSender(
    private val commandSender: net.md_5.bungee.api.CommandSender,
    private val plugin: ProxyBungeeCordPlugin
) : CommandSender {

    fun getCommandSender(): net.md_5.bungee.api.CommandSender {
        return commandSender
    }

    override fun sendMessage(message: String) {
        val player = commandSender as? ProxiedPlayer
        val component = plugin.deserializeToComponent(message, player)
        plugin.adventure().sender(commandSender).sendMessage(component)
    }
}

fun Component.toBaseComponent(): BaseComponent {
    return BungeeComponentSerializer.get().serialize(this)[0]
}