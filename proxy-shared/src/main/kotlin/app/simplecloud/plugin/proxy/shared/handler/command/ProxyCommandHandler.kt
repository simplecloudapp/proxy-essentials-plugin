package app.simplecloud.plugin.proxy.shared.handler.command

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import app.simplecloud.plugin.proxy.shared.handler.MotdLayoutHandler
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.incendo.cloud.suggestion.Suggestion
import java.util.concurrent.CompletableFuture

class ProxyCommandHandler<C : CommandSender>(
    val commandManager: CommandManager<C>,
    val proxyPlugin: ProxyPlugin
) {

    fun loadCommands() {
        loadToggleMaintenanceCommand()
        loadActivateMaintenanceCommand()
        loadDeactivateMaintenanceCommand()

        loadLayoutMaintenanceSetCommand()
        loadLayoutNonMaintenanceSetCommand()
    }

    private fun loadToggleMaintenanceCommand() {
        commandManager.command(
            commandManager.commandBuilder("proxy")
                .literal("maintenance")
                .literal("toggle")
                .permission("simplecloud.command.proxy.maintenance.toggle")
                .handler { context: CommandContext<C> ->
                    runBlocking {
                        val mode = !proxyPlugin.cloudControllerHandler.getGroupProperties("maintenance").toBoolean()
                        setProxyMaintenanceMode(mode)
                        context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.toggleMaintenance)
                    }
                }
                .build()
        )
    }

    private fun loadActivateMaintenanceCommand() {
        commandManager.command(
            commandManager.commandBuilder("proxy")
                .literal("maintenance")
                .literal("activate")
                .permission("simplecloud.command.proxy.maintenance.activate")
                .handler { context: CommandContext<C> ->
                    runBlocking {
                        if (!proxyPlugin.cloudControllerHandler.getGroupProperties("maintenance").toBoolean()) {
                            setProxyMaintenanceMode(true)
                            context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.activateMaintenance)
                        } else {
                            context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.maintenanceAlreadyActivated)
                        }
                    }
                }
                .build()
        )
    }

    private fun loadDeactivateMaintenanceCommand() {
        commandManager.command(
            commandManager.commandBuilder("proxy")
                .literal("maintenance")
                .literal("deactivate")
                .permission("simplecloud.command.proxy.maintenance.deactivate")
                .handler { context: CommandContext<C> ->
                    runBlocking {
                        if (proxyPlugin.cloudControllerHandler.getGroupProperties("maintenance").toBoolean()) {
                            setProxyMaintenanceMode(false)
                            context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.deactivateMaintenance)
                        } else {
                            context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.maintenanceAlreadyDeactivated)
                        }
                    }
                }
                .build()
        )
    }

    private fun loadLayoutMaintenanceSetCommand() {
        commandManager.command(
            commandManager.commandBuilder("proxy")
                .literal("layout")
                .literal("maintenance")
                .literal("set")
                .required(
                    "layout",
                    stringParser()
                ) { _, _ ->
                    val suggestionList = proxyPlugin.motdLayoutHandler.getAllMaintenanceLayouts().map { Suggestion.suggestion(it) }
                    CompletableFuture.completedFuture(suggestionList)
                }
                .permission("simplecloud.command.proxy.layout.maintenance.set")
                .handler { context: CommandContext<C> ->
                    val layout = context.get<String>("layout")

                    if (!proxyPlugin.motdLayoutHandler.getAllMaintenanceLayouts().contains(layout)) {
                        context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.layoutNotFound)
                        return@handler
                    }

                    if (MotdLayoutHandler.CURRENT_MAINTENANCE_LAYOUT_KEY == layout) {
                        context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.layoutMaintenanceAlreadySet)
                        return@handler
                    }

                    runBlocking {
                        proxyPlugin.cloudControllerHandler.setServicePropertiesOnAllGroupServices(MotdLayoutHandler.CURRENT_MAINTENANCE_LAYOUT_KEY, layout)
                        proxyPlugin.cloudControllerHandler.setGroupProperties(MotdLayoutHandler.CURRENT_MAINTENANCE_LAYOUT_KEY, layout)

                        context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.layoutMaintenanceSet)
                    }
                }
                .build()
        )
    }

    private fun loadLayoutNonMaintenanceSetCommand() {
        commandManager.command(
            commandManager.commandBuilder("proxy")
                .literal("layout")
                .literal("nonmaintenance")
                .literal("set")
                .required(
                    "layout",
                    stringParser()
                ) { _, _ ->
                    val suggestionList = proxyPlugin.motdLayoutHandler.getAllNoneMaintenanceLayouts().map { Suggestion.suggestion(it) }
                    CompletableFuture.completedFuture(suggestionList)
                }
                .permission("simplecloud.command.proxy.layout.nonmaintenance.set")
                .handler { context: CommandContext<C> ->
                    val layout = context.get<String>("layout")

                    if (!proxyPlugin.motdLayoutHandler.getAllNoneMaintenanceLayouts().contains(layout)) {
                        context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.layoutNotFound)
                        return@handler
                    }

                    if (MotdLayoutHandler.CURRENT_LAYOUT_KEY == layout) {
                        context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.layoutNonMaintenanceAlreadySet)
                        return@handler
                    }

                    runBlocking {
                        proxyPlugin.cloudControllerHandler.setServicePropertiesOnAllGroupServices(MotdLayoutHandler.CURRENT_LAYOUT_KEY, layout)
                        proxyPlugin.cloudControllerHandler.setGroupProperties(MotdLayoutHandler.CURRENT_LAYOUT_KEY, layout)

                        context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.layoutNonMaintenanceSet)
                    }
                }
                .build()
        )
    }

    private suspend fun setProxyMaintenanceMode(mode: Boolean) {
        this.proxyPlugin.cloudControllerHandler.setGroupProperties("maintenance", mode.toString())
        this.proxyPlugin.cloudControllerHandler.setServicePropertiesOnAllGroupServices("maintenance", mode.toString())
    }
}