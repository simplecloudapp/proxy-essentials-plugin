package app.simplecloud.plugin.proxy.shared.command.commands

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import app.simplecloud.plugin.proxy.shared.command.ProxyCommandSender
import app.simplecloud.plugin.proxy.shared.config.MessageConfig
import app.simplecloud.plugin.proxy.shared.motd.MotdLayoutRepository
import app.simplecloud.plugin.proxy.shared.utilities.ProxyPermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.suggestion.Suggestion
import java.util.concurrent.CompletableFuture


class LayoutCommandHandler<C : ProxyCommandSender>(
    private val manager: CommandManager<C>,
    private val plugin: ProxyPlugin
) {
    private val commands = listOf(
        "/scproxy layout help",
        "/scproxy layout info [group]",
        "/scproxy layout set <group> <layout>"
    )

    fun loadCommands() {
        loadHelp()
        loadInfoDefault()
        loadInfoGroup()
        loadSet()
    }

    private fun loadHelp() {
        manager.command(
            manager.commandBuilder("scproxy")
                .literal("layout")
                .permission(ProxyPermissions.LAYOUT_HELP)
                .handler { context: CommandContext<C> -> handleHelp(context.sender()) }
                .build()
        )
        manager.command(
            manager.commandBuilder("scproxy")
                .literal("layout")
                .literal("help")
                .permission(ProxyPermissions.LAYOUT_HELP)
                .handler { context: CommandContext<C> -> handleHelp(context.sender()) }
                .build()
        )
    }

    private fun handleHelp(sender: ProxyCommandSender) {
        val messages = plugin.messageConfig.get()
        val entry = messages.command.layout.help.entry

        sender.sendMessage(messages.resolve(messages.command.layout.help.header))
        commands.forEach { command ->
            sender.sendMessage(entry.replace("<command>", command))
        }
    }

    private fun loadInfoDefault() {
        manager.command(
            manager.commandBuilder("scproxy")
                .literal("layout")
                .literal("info")
                .permission(ProxyPermissions.LAYOUT_INFO)
                .handler { context: CommandContext<C> ->
                    sendLayoutInfo(
                        context.sender(),
                        group = "default",
                        layoutName = plugin.config.get().initialLayout
                    )
                }
                .build()
        )
    }

    private fun loadInfoGroup() {
        manager.command(
            manager.commandBuilder("scproxy")
                .literal("layout")
                .literal("info")
                .required("group", StringParser.stringParser()) { _, _ -> suggestGroups() }
                .permission(ProxyPermissions.LAYOUT_INFO)
                .handler { context: CommandContext<C> ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val group = context.get<String>("group")
                        sendLayoutInfo(context.sender(), group, resolveLayoutOfGroup(group))
                    }
                }
                .build()
        )
    }

    private suspend fun resolveLayoutOfGroup(group: String): String {
        val groupLayout = plugin.cloudControllerHandler
            .getGroupProperty(group, MotdLayoutRepository.KEY)
        if (groupLayout != null) {
            return groupLayout
        }

        val config = plugin.config.get()
        val stateName = plugin.joinStateService.getJoinStateAtGroup(group)

        return config.joinstates.find { it.name == stateName }?.forcedMotdLayout ?: config.initialLayout
    }

    private fun sendLayoutInfo(sender: ProxyCommandSender, group: String, layoutName: String) {
        val messages = plugin.messageConfig.get()

        sender.sendMessage(messages.resolve(messages.command.layout.info.header).replace("<group>", group))
        sender.sendMessage(messages.resolve(messages.command.layout.info.entry).replace("<layout>", layoutName))
    }

    private fun loadSet() {
        manager.command(
            manager.commandBuilder("scproxy")
                .literal("layout")
                .literal("set")
                .required("group", StringParser.stringParser()) { _, _ -> suggestGroups() }
                .required("layout", StringParser.stringParser()) { _, _ -> suggestLayouts() }
                .permission(ProxyPermissions.LAYOUT_SET)
                .handler { context: CommandContext<C> ->
                    CoroutineScope(Dispatchers.IO).launch { handleSet(context) }
                }
                .build()
        )
    }

    private suspend fun handleSet(context: CommandContext<C>) {
        val group = context.get<String>("group")
        val layout = context.get<String>("layout")
        val messages = plugin.messageConfig.get()

        if (layout !in plugin.layoutRepository.getAvailableLayouts()) {
            sendSetResult(context.sender(), messages, updated = false)
            return
        }

        val groupUpdated = plugin.cloudControllerHandler
            .updateGroupProperty(group, MotdLayoutRepository.KEY, layout)
        val servicesUpdated = plugin.cloudControllerHandler
            .updateServicePropertyOnAllGroupServers(group, MotdLayoutRepository.KEY, layout)
        val updated = groupUpdated && servicesUpdated

        if (updated && isCurrentGroup(group)) {
            plugin.layoutRepository.setLocalLayout(layout)
        }

        sendSetResult(context.sender(), messages, updated)
    }

    private fun sendSetResult(sender: ProxyCommandSender, messages: MessageConfig, updated: Boolean) {
        val message = when {
            updated -> messages.command.layout.set.updateSuccess
            else -> messages.command.layout.set.updateFailure
        }

        sender.sendMessage(messages.resolve(message))
    }

    private fun isCurrentGroup(group: String): Boolean {
        val currentServer = plugin.cloudControllerHandler.currentServer ?: return false
        return currentServer.isFromGroup && currentServer.group?.name == group
    }

    private fun suggestGroups(): CompletableFuture<List<Suggestion>> = runBlocking {
        val groups = plugin.cloudControllerHandler.getAllGroups()
            .map { Suggestion.suggestion(it) }

        CompletableFuture.completedFuture(groups)
    }

    private fun suggestLayouts(): CompletableFuture<List<Suggestion>> {
        val layouts = plugin.layoutRepository.getAvailableLayouts()
            .map { Suggestion.suggestion(it) }

        return CompletableFuture.completedFuture(layouts)
    }
}
