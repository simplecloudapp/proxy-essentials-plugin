package app.simplecloud.plugin.proxy.shared.handler.command

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import app.simplecloud.plugin.proxy.shared.handler.MotdLayoutHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.suggestion.Suggestion
import java.util.concurrent.CompletableFuture

class LayoutCommandHandler<C : CommandSender>(
    private val commandManager: CommandManager<C>,
    private val proxyPlugin: ProxyPlugin
) {

    companion object {
        const val MOTD_LAYOUT_KEY = MotdLayoutHandler.MOTD_LAYOUT_KEY
    }

    fun loadCommands() {
        loadHelp()
        loadInfoDefault()
        loadInfoGroup()
        loadSet()
    }

    private fun loadHelp() {
        commandManager.command(
            commandManager.commandBuilder("scproxy")
                .literal("layout")
                .permission("simplecloud.proxy-essentials.command.layout.help")
                .handler { context: CommandContext<C> -> handleHelp(context) }
                .build()
        )
        commandManager.command(
            commandManager.commandBuilder("scproxy")
                .literal("layout")
                .literal("help")
                .permission("simplecloud.proxy-essentials.command.layout.help")
                .handler { context: CommandContext<C> -> handleHelp(context) }
                .build()
        )
    }

    private fun handleHelp(context: CommandContext<C>) {
        val msgs = proxyPlugin.messagesConfiguration.get()
        val entry = msgs.command.layout.help.entry
        context.sender().sendMessage(msgs.resolve(msgs.command.layout.help.header))
        context.sender().sendMessage(entry.replace("<command>", "/scproxy layout help"))
        context.sender().sendMessage(entry.replace("<command>", "/scproxy layout info [group]"))
        context.sender().sendMessage(entry.replace("<command>", "/scproxy layout set <group> <layout>"))
    }

    private fun loadInfoDefault() {
        commandManager.command(
            commandManager.commandBuilder("scproxy")
                .literal("layout")
                .literal("info")
                .permission("simplecloud.proxy-essentials.command.layout.info")
                .handler { context: CommandContext<C> ->
                    val config = proxyPlugin.proxyEssentialsConfig.get()
                    val msgs = proxyPlugin.messagesConfiguration.get()
                    val layoutName = config.initialLayout
                    context.sender().sendMessage(
                        msgs.resolve(msgs.command.layout.info.header).replace("<group>", "default")
                    )
                    context.sender().sendMessage(
                        msgs.resolve(msgs.command.layout.info.entry).replace("<layout>", layoutName)
                    )
                }
                .build()
        )
    }

    private fun loadInfoGroup() {
        commandManager.command(
            commandManager.commandBuilder("scproxy")
                .literal("layout")
                .literal("info")
                .required("group", StringParser.stringParser()) { _, _ ->
                    runBlocking {
                        val groups = proxyPlugin.cloudControllerHandler.getAllGroups()
                            .map { Suggestion.suggestion(it) }
                        CompletableFuture.completedFuture(groups)
                    }
                }
                .permission("simplecloud.proxy-essentials.command.layout.info")
                .handler { context: CommandContext<C> ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val group = context.get<String>("group")
                        val msgs = proxyPlugin.messagesConfiguration.get()

                        val layoutName = proxyPlugin.cloudControllerHandler.getGroupProperty(group, MOTD_LAYOUT_KEY)
                            ?: proxyPlugin.proxyEssentialsConfig.get().let { config ->
                                val stateName = proxyPlugin.joinStateHandler.getJoinStateAtGroup(group)
                                config.joinstates.find { it.name == stateName }?.forcedMotdLayout
                                    ?: config.initialLayout
                            }

                        context.sender().sendMessage(
                            msgs.resolve(msgs.command.layout.info.header).replace("<group>", group)
                        )
                        context.sender().sendMessage(
                            msgs.resolve(msgs.command.layout.info.entry).replace("<layout>", layoutName)
                        )
                    }
                }
                .build()
        )
    }

    private fun loadSet() {
        commandManager.command(
            commandManager.commandBuilder("scproxy")
                .literal("layout")
                .literal("set")
                .required("group", StringParser.stringParser()) { _, _ ->
                    runBlocking {
                        val groups = proxyPlugin.cloudControllerHandler.getAllGroups()
                            .map { Suggestion.suggestion(it) }
                        CompletableFuture.completedFuture(groups)
                    }
                }
                .required("layout", StringParser.stringParser()) { _, _ ->
                    val layouts = proxyPlugin.motdLayoutHandler.getAvailableLayouts()
                        .map { Suggestion.suggestion(it) }
                    CompletableFuture.completedFuture(layouts)
                }
                .permission("simplecloud.proxy-essentials.command.layout.set")
                .handler { context: CommandContext<C> ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val group = context.get<String>("group")
                        val layout = context.get<String>("layout")
                        val msgs = proxyPlugin.messagesConfiguration.get()
                        val availableLayouts = proxyPlugin.motdLayoutHandler.getAvailableLayouts()

                        if (layout !in availableLayouts) {
                            context.sender().sendMessage(msgs.resolve(msgs.command.layout.set.updateFailure))
                            return@launch
                        }

                        val groupUpdated = proxyPlugin.cloudControllerHandler.updateGroupProperty(group, MOTD_LAYOUT_KEY, layout)
                        val servicesUpdated = proxyPlugin.cloudControllerHandler.updateServicePropertyOnAllGroupServers(
                            group,
                            MOTD_LAYOUT_KEY,
                            layout
                        )
                        val success = groupUpdated && servicesUpdated

                        if (success) {
                            val currentServer = proxyPlugin.cloudControllerHandler.currentServer
                            if (currentServer?.isFromGroup == true && currentServer.group?.name == group) {
                                proxyPlugin.motdLayoutHandler.setLocalLayout(layout)
                            }
                            context.sender().sendMessage(msgs.resolve(msgs.command.layout.set.updateSuccess))
                        } else {
                            context.sender().sendMessage(msgs.resolve(msgs.command.layout.set.updateFailure))
                        }
                    }
                }
                .build()
        )
    }
}
