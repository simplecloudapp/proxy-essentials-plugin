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
        loadReloadCommand()
    }

    private fun loadReloadCommand() {
        commandManager.command(
            commandManager.commandBuilder("proxyessentials")
                .literal("reload")
                .permission("simplecloud.command.proxyessentials.reload")
                .handler { context: CommandContext<C> ->

                    val sender = context.sender()
                    sender.sendMessage(proxyPlugin.messagesConfiguration.get().commandMessage.configReloading)

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            proxyPlugin.tabListConfiguration.reload()
                            proxyPlugin.placeHolderConfiguration.reload()
                            proxyPlugin.messagesConfiguration.reload()
                            proxyPlugin.joinStateConfiguration.reload()

                            proxyPlugin.motdLayoutHandler.loadMotdLayouts()


                            sender.sendMessage(proxyPlugin.messagesConfiguration.get().commandMessage.configReloadedSuccess)
                        } catch (e: Exception) {
                            sender.sendMessage(proxyPlugin.messagesConfiguration.get().commandMessage.configReloadedFailure
                                .replace("<errorMessage>", e.message ?: "Unknown error"))
                            logger.log(Level.SEVERE, "Error reloading configuration", e)
                        }
                    }
                }
                .build()
        )
    }
}
