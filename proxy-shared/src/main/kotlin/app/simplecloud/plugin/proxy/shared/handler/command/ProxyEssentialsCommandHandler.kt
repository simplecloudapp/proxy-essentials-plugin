package app.simplecloud.plugin.proxy.shared.handler.command

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import java.util.logging.Level
import java.util.logging.Logger

class ProxyEssentialsCommandHandler<C : CommandSender>(
    private val commandManager: CommandManager<C>,
    private val proxyPlugin: ProxyPlugin
) {
    private val logger = Logger.getLogger(ProxyEssentialsCommandHandler::class.java.name)

    fun loadCommands() {
        loadHelp()
        loadReload()
    }

    private fun loadHelp() {
        commandManager.command(
            commandManager.commandBuilder("scproxy")
                .permission("simplecloud.proxy-essentials.command.help")
                .handler { context: CommandContext<C> -> sendHelp(context) }
                .build()
        )
        commandManager.command(
            commandManager.commandBuilder("scproxy")
                .literal("help")
                .permission("simplecloud.proxy-essentials.command.help")
                .handler { context: CommandContext<C> -> sendHelp(context) }
                .build()
        )
    }

    private fun sendHelp(context: CommandContext<C>) {
        val sender = context.sender()
        val msgs = proxyPlugin.messagesConfiguration.get()
        val helpEntry = msgs.command.joinState.help.entry
        sender.sendMessage(msgs.resolve("<prefix>Available /scproxy commands:"))
        sender.sendMessage(helpEntry.replace("<command>", "/scproxy help"))
        sender.sendMessage(helpEntry.replace("<command>", "/scproxy joinstate help"))
        sender.sendMessage(helpEntry.replace("<command>", "/scproxy joinstate info <group>"))
        sender.sendMessage(helpEntry.replace("<command>", "/scproxy joinstate info <group> <id>"))
        sender.sendMessage(helpEntry.replace("<command>", "/scproxy joinstate set <group> <joinstate>"))
        sender.sendMessage(helpEntry.replace("<command>", "/scproxy joinstate set <group> <id> <joinstate>"))
        sender.sendMessage(helpEntry.replace("<command>", "/scproxy layout help"))
        sender.sendMessage(helpEntry.replace("<command>", "/scproxy layout info [group]"))
        sender.sendMessage(helpEntry.replace("<command>", "/scproxy layout set <group> <layout>"))
    }

    private fun loadReload() {
        commandManager.command(
            commandManager.commandBuilder("scproxy")
                .literal("reload")
                .permission("simplecloud.proxy-essentials.command.help")
                .handler { context: CommandContext<C> ->
                    val sender = context.sender()

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            proxyPlugin.messagesConfiguration.reload()
                            val messages = proxyPlugin.messagesConfiguration.get()
                            sender.sendMessage(messages.resolve(messages.command.reload.start))

                            proxyPlugin.proxyEssentialsConfig.reload()
                            proxyPlugin.placeHolderConfiguration.reload()
                            proxyPlugin.motdLayoutHandler.loadMotdLayouts()

                            sender.sendMessage(messages.resolve(messages.command.reload.success))
                        } catch (e: Exception) {
                            val messages = proxyPlugin.messagesConfiguration.get()
                            sender.sendMessage(
                                messages.resolve(messages.command.reload.failure)
                                    .replace("<error>", e.message ?: "Unknown error")
                            )
                            logger.log(Level.SEVERE, "Error reloading configuration", e)
                        }
                    }
                }
                .build()
        )
    }
}
