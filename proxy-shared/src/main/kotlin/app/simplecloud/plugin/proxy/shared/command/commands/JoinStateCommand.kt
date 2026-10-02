package app.simplecloud.plugin.proxy.shared.command.commands

import app.simplecloud.api.CloudApi
import app.simplecloud.plugin.proxy.shared.ProxyEssentials
import app.simplecloud.plugin.proxy.shared.command.ProxySender
import app.simplecloud.plugin.proxy.shared.command.ProxySuggestions
import app.simplecloud.plugin.proxy.shared.utilities.CommandPermissions
import kotlinx.coroutines.future.await
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.kotlin.coroutines.extension.suspendingHandler
import org.incendo.cloud.parser.standard.IntegerParser.integerParser
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.incendo.cloud.permission.Permission
import org.slf4j.LoggerFactory

class JoinStateCommand<C : ProxySender>(
    private val api: CloudApi,
    private val plugin: ProxyEssentials,
    private val manager: CommandManager<C>
) {

    private val logger = LoggerFactory.getLogger(JoinStateCommand::class.java)

    fun register() {
        registerHelp()
        registerList()
        registerInfo()
        registerSet()
    }

    private fun registerHelp() {
        manager.command(
            manager.commandBuilder("scproxy")
                .literal("joinstate")
                .handler { context -> sendHelp(context) }
                .permission(Permission.permission(CommandPermissions.JOINSTATE_HELP))
                .build()
        )
        manager.command(
            manager.commandBuilder("scproxy")
                .literal("joinstate")
                .literal("help")
                .handler { context -> sendHelp(context) }
                .permission(Permission.permission(CommandPermissions.JOINSTATE_HELP))
                .build()
        )
    }

    private fun registerList() {
        manager.command(
            manager.commandBuilder("scproxy")
                .literal("joinstate")
                .literal("list")
                .handler { context ->
                    val sender = context.sender()
                    val states = plugin.messages.get().command.joinState.list.states
                    sender.sendMessage(plugin.messageFormatter.format(states.header))
                    plugin.config.get().joinstates.forEach {
                        val state = Placeholder.unparsed("state", it.name)
                        val permission = Placeholder.unparsed("joinPermission", it.permission.join.ifBlank { "-" })
                        sender.sendMessage(plugin.messageFormatter.format(states.entry, state, permission))
                    }
                }
                .permission(Permission.permission(CommandPermissions.JOINSTATE_INFO))
                .build()
        )
    }

    private fun registerInfo() {
        manager.command(
            manager.commandBuilder("scproxy")
                .literal("joinstate")
                .literal("info")
                .suspendingHandler { context ->
                    val sender = context.sender()
                    try {
                        sender.sendMessage(plugin.messageFormatter.format(plugin.messages.get().command.joinState.list.groups.header))
                        api.group().allGroups.await().forEach {
                            sendState(sender, it.name, plugin.joinStateService.getGroupState(it.name))
                        }
                        api.persistentServer().allPersistentServers.await().forEach {
                            sendState(sender, it.name, plugin.joinStateService.getPersistentServerState(it.name))
                        }
                    } catch (e: Exception) {
                        logger.error("Could not list the join states", e)
                        sender.sendMessage(plugin.messageFormatter.format(plugin.messages.get().command.failure))
                    }
                }
                .permission(Permission.permission(CommandPermissions.JOINSTATE_INFO))
                .build()
        )
        manager.command(
            manager.commandBuilder("scproxy")
                .literal("joinstate")
                .literal("info")
                .required("targetType", stringParser(), ProxySuggestions.targetTypes())
                .required("target", stringParser(), ProxySuggestions.targets(api))
                .optional("id", integerParser(1), ProxySuggestions.serverIds(api))
                .suspendingHandler { context ->
                    val sender = context.sender()
                    val targetType = context.get<String>("targetType")
                    val target = context.get<String>("target")
                    val id = context.getOrDefault("id", null as Int?)
                    val messages = plugin.messages.get().command
                    try {
                        if (!isTargetTypeValid(targetType)) {
                            sender.sendMessage(plugin.messageFormatter.format(messages.invalidTargetType))
                            return@suspendingHandler
                        }

                        when {
                            targetType.equals("ps", true) -> sendState(sender, target, plugin.joinStateService.getPersistentServerState(target))
                            id != null -> sendState(sender, "$target-$id", plugin.joinStateService.getServiceState(target, id))
                            else -> sendState(sender, target, plugin.joinStateService.getGroupState(target))
                        }
                    } catch (e: Exception) {
                        logger.error("Could not load the join state of '$target'", e)
                        sender.sendMessage(plugin.messageFormatter.format(messages.failure))
                    }
                }
                .permission(Permission.permission(CommandPermissions.JOINSTATE_INFO))
                .build()
        )
    }

    private fun registerSet() {
        manager.command(
            manager.commandBuilder("scproxy")
                .literal("joinstate")
                .literal("set")
                .required("targetType", stringParser(), ProxySuggestions.targetTypes())
                .required("target", stringParser(), ProxySuggestions.targets(api))
                .required("joinstate", stringParser(), ProxySuggestions.joinStates(plugin.config))
                .optional("id", integerParser(1), ProxySuggestions.serverIds(api))
                .suspendingHandler { context ->
                    val sender = context.sender()
                    val targetType = context.get<String>("targetType")
                    val target = context.get<String>("target")
                    val state = context.get<String>("joinstate")
                    val id = context.getOrDefault("id", null as Int?)
                    val messages = plugin.messages.get().command
                    val isGroup = targetType.equals("group", true) && id == null
                    val updateMessages = if (isGroup) messages.joinState.group else messages.joinState.server

                    if (!isTargetTypeValid(targetType)) {
                        sender.sendMessage(plugin.messageFormatter.format(messages.invalidTargetType))
                        return@suspendingHandler
                    }

                    if (plugin.config.get().joinstates.none { it.name == state }) {
                        sender.sendMessage(plugin.messageFormatter.format(updateMessages.updateFailure))
                        return@suspendingHandler
                    }

                    try {
                        val currentState = when {
                            targetType.equals("ps", true) -> plugin.joinStateService.getPersistentServerState(target)
                            id != null -> plugin.joinStateService.getServiceState(target, id)
                            else -> plugin.joinStateService.getGroupState(target)
                        }
                        if (currentState == state) {
                            sender.sendMessage(plugin.messageFormatter.format(updateMessages.updateNoChange))
                            return@suspendingHandler
                        }

                        when {
                            targetType.equals("ps", true) -> plugin.joinStateService.setPersistentServerState(target, state)
                            id != null -> plugin.joinStateService.setServiceState(target, id, state)
                            else -> plugin.joinStateService.setGroupState(target, state)
                        }
                        sender.sendMessage(plugin.messageFormatter.format(updateMessages.updateSuccess))
                    } catch (e: Exception) {
                        logger.error("Could not set the join state of '$target' to '$state'", e)
                        sender.sendMessage(plugin.messageFormatter.format(updateMessages.updateFailure))
                    }
                }
                .permission(Permission.permission(CommandPermissions.JOINSTATE_SET))
                .build()
        )
    }

    private fun sendHelp(context: CommandContext<C>) {
        val sender = context.sender()
        val help = plugin.messages.get().command.joinState.help
        val commands = listOf(
            "/scproxy joinstate help",
            "/scproxy joinstate list",
            "/scproxy joinstate info",
            "/scproxy joinstate info <group|ps> <target> [id]",
            "/scproxy joinstate set <group|ps> <target> <joinstate> [id]"
        )

        sender.sendMessage(plugin.messageFormatter.format(help.header))
        commands.forEach { sender.sendMessage(plugin.messageFormatter.format(help.entry, Placeholder.unparsed("command", it))) }
    }

    private fun sendState(sender: C, target: String, state: String) {
        val entry = plugin.messages.get().command.joinState.list.groups.entry
        sender.sendMessage(plugin.messageFormatter.format(entry, Placeholder.unparsed("group", target), Placeholder.unparsed("state", state)))
    }

    private fun isTargetTypeValid(targetType: String): Boolean {
        return targetType.equals("group", true) || targetType.equals("ps", true)
    }
}
