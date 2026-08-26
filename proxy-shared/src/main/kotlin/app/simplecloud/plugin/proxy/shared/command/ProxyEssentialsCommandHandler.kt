package app.simplecloud.plugin.proxy.shared.command

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import java.util.logging.Level
import java.util.logging.Logger

class ProxyEssentialsCommandHandler<C : ProxyCommandSender>(
    private val manager: CommandManager<C>,
    private val plugin: ProxyPlugin
) {

    private val logger = Logger.getLogger(ProxyEssentialsCommandHandler::class.java.name)
    private val commands = listOf(
        "/scproxy help",
        "/scproxy reload",
        "/scproxy joinstate help",
        "/scproxy joinstate info <group>",
        "/scproxy joinstate info <group> <id>",
        "/scproxy joinstate set <group> <joinstate>",
        "/scproxy joinstate set <group> <id> <joinstate>",
        "/scproxy layout help",
        "/scproxy layout info [group]",
        "/scproxy layout set <group> <layout>"
    )

    fun loadCommands() {
        loadHelp()
        loadReload()
    }

    private fun loadHelp() {
        manager.command(
            manager.commandBuilder("scproxy")
                .permission("simplecloud.proxy-essentials.command.help")
                .handler { context: CommandContext<C> -> sendHelp(context.sender()) }
                .build()
        )
        manager.command(
            manager.commandBuilder("scproxy")
                .literal("help")
                .permission("simplecloud.proxy-essentials.command.help")
                .handler { context: CommandContext<C> -> sendHelp(context.sender()) }
                .build()
        )
    }

    private fun sendHelp(sender: C) {
        val messages = plugin.messageConfig.get()
        val entry = messages.command.joinState.help.entry

        sender.sendMessage(messages.resolve("<prefix>Available /scproxy commands:"))
        commands.forEach { command ->
            sender.sendMessage(entry.replace("<command>", command))
        }
    }

    private fun loadReload() {
        manager.command(
            manager.commandBuilder("scproxy")
                .literal("reload")
                .permission("simplecloud.proxy-essentials.command.reload")
                .handler { context: CommandContext<C> -> reload(context.sender()) }
                .build()
        )
    }

    private fun reload(sender: C) {
        val messagesBeforeReload = plugin.messageConfig.get()
        sender.sendMessage(messagesBeforeReload.resolve(messagesBeforeReload.command.reload.start))

        CoroutineScope(Dispatchers.IO).launch {
            try {
                plugin.reload()
                val messages = plugin.messageConfig.get()
                sender.sendMessage(messages.resolve(messages.command.reload.success))
            } catch (e: Exception) {
                val messages = plugin.messageConfig.get()
                sender.sendMessage(messages.resolve(messages.command.reload.failure).replace("<error>", e.message ?: "Unknown error"))
                logger.log(Level.SEVERE, "Error reloading configuration", e)
            }
        }
    }
}
