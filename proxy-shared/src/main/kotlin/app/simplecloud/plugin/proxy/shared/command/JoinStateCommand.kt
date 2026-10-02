package app.simplecloud.plugin.proxy.shared.command

import app.simplecloud.api.CloudApi
import app.simplecloud.plugin.api.shared.config.ConfigurationFactory
import app.simplecloud.plugin.proxy.shared.config.MessageConfig
import app.simplecloud.plugin.proxy.shared.config.ProxyEssentialsConfig
import app.simplecloud.plugin.proxy.shared.config.UpdateMessages
import app.simplecloud.plugin.proxy.shared.joinstate.JoinStateService
import app.simplecloud.plugin.proxy.shared.utilities.CommandPermissions
import app.simplecloud.plugin.proxy.shared.utilities.text.MessageFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.parser.standard.IntegerParser
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.suggestion.Suggestion
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

class JoinStateCommand<C : ProxySender>(
    private val manager: CommandManager<C>,
    private val api: CloudApi,
    private val joinStateService: JoinStateService,
    private val config: ConfigurationFactory<ProxyEssentialsConfig>,
    private val messages: ConfigurationFactory<MessageConfig>,
    private val formatter: MessageFormatter,
    private val scope: CoroutineScope
) {

    private val logger = LoggerFactory.getLogger(JoinStateCommand::class.java)

    private val commands = listOf(
        "/scproxy joinstate help",
        "/scproxy joinstate list",
        "/scproxy joinstate info [group]",
        "/scproxy joinstate info <group> <id>",
        "/scproxy joinstate set <group> <joinstate>",
        "/scproxy joinstate set <group> <id> <joinstate>",
        "<group> can also be the name of a persistent server"
    )

    fun register() {
        val root = manager.commandBuilder("scproxy").literal("joinstate")

        manager.command(root.permission(CommandPermissions.JOINSTATE_HELP).handler { sendHelp(it.sender()) })
        manager.command(root.literal("help").permission(CommandPermissions.JOINSTATE_HELP).handler { sendHelp(it.sender()) })
        manager.command(root.literal("list").permission(CommandPermissions.JOINSTATE_INFO).handler { sendStates(it.sender()) })
        manager.command(root.literal("info").permission(CommandPermissions.JOINSTATE_INFO).handler { sendAllTargets(it.sender()) })
        manager.command(
            root.literal("info")
                .required("group", StringParser.stringParser()) { _, _ -> suggestTargets() }
                .optional("id", IntegerParser.integerParser(1)) { context, _ -> suggestNumericalIds(context) }
                .permission(CommandPermissions.JOINSTATE_INFO)
                .handler { sendInfo(it) }
        )
        manager.command(
            root.literal("set")
                .required("group", StringParser.stringParser()) { _, _ -> suggestTargets() }
                .required("idOrJoinstate", StringParser.stringParser()) { context, _ -> suggestIdsAndStates(context) }
                .optional("joinstate", StringParser.stringParser()) { _, _ -> suggestStates() }
                .permission(CommandPermissions.JOINSTATE_SET)
                .handler { set(it) }
        )
    }

    private fun sendHelp(sender: C) {
        val help = messages.get().command.joinState.help
        sender.sendMessage(formatter.format(help.header))
        commands.forEach { sender.sendMessage(formatter.format(help.entry, Placeholder.unparsed("command", it))) }
    }

    private fun sendStates(sender: C) {
        val states = messages.get().command.joinState.list.states
        sender.sendMessage(formatter.format(states.header))
        config.get().joinstates.forEach {
            val permission = Placeholder.unparsed("joinPermission", it.permission.join.ifBlank { "-" })
            sender.sendMessage(formatter.format(states.entry, Placeholder.unparsed("state", it.name), permission))
        }
    }

    private fun sendAllTargets(sender: C) {
        runAsync(sender, messages.get().command.failure) {
            sender.sendMessage(formatter.format(messages.get().command.joinState.list.groups.header))
            getTargets().forEach { sendState(sender, it, joinStateService.getTargetState(it)) }
        }
    }

    private fun sendInfo(context: CommandContext<C>) {
        val target = context.get<String>("group")
        val id = context.optional<Int>("id")

        runAsync(context.sender(), messages.get().command.failure) {
            if (id.isPresent) {
                sendState(context.sender(), "$target-${id.get()}", joinStateService.getServiceState(target, id.get()))
            } else {
                sendState(context.sender(), target, joinStateService.getTargetState(target))
            }
        }
    }

    private fun sendState(sender: C, target: String, state: String) {
        val entry = messages.get().command.joinState.list.groups.entry
        sender.sendMessage(formatter.format(entry, Placeholder.unparsed("group", target), Placeholder.unparsed("state", state)))
    }

    private fun set(context: CommandContext<C>) {
        val target = context.get<String>("group")
        val idOrState = context.get<String>("idOrJoinstate")
        val state = context.optional<String>("joinstate")

        if (!state.isPresent) {
            setTargetState(context.sender(), target, idOrState)
            return
        }

        val numericalId = idOrState.toIntOrNull()
        if (numericalId == null) {
            context.sender().sendMessage(formatter.format(messages.get().command.joinState.server.updateFailure))
            return
        }

        setServiceState(context.sender(), target, numericalId, state.get())
    }

    private fun setTargetState(sender: C, target: String, state: String) {
        val groupMessages = messages.get().command.joinState.group
        if (config.get().joinstates.none { it.name == state }) {
            sender.sendMessage(formatter.format(groupMessages.updateFailure))
            return
        }

        runAsync(sender, groupMessages.updateFailure) {
            if (joinStateService.isGroup(target)) {
                updateState(sender, groupMessages, joinStateService.getGroupState(target), state) {
                    joinStateService.setGroupState(target, state)
                }
            } else {
                updateState(sender, messages.get().command.joinState.server, joinStateService.getPersistentServerState(target), state) {
                    joinStateService.setPersistentServerState(target, state)
                }
            }
        }
    }

    private fun setServiceState(sender: C, groupName: String, numericalId: Int, state: String) {
        val serverMessages = messages.get().command.joinState.server
        if (config.get().joinstates.none { it.name == state }) {
            sender.sendMessage(formatter.format(serverMessages.updateFailure))
            return
        }

        runAsync(sender, serverMessages.updateFailure) {
            updateState(sender, serverMessages, joinStateService.getServiceState(groupName, numericalId), state) {
                joinStateService.setServiceState(groupName, numericalId, state)
            }
        }
    }

    private suspend fun updateState(sender: C, updateMessages: UpdateMessages, currentState: String, state: String, update: suspend () -> Unit) {
        if (currentState == state) {
            sender.sendMessage(formatter.format(updateMessages.updateNoChange))
            return
        }

        update()
        sender.sendMessage(formatter.format(updateMessages.updateSuccess))
    }

    private fun runAsync(sender: C, failureMessage: String, block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
            } catch (e: Exception) {
                logger.error("The join state command failed", e)
                sender.sendMessage(formatter.format(failureMessage))
            }
        }
    }

    private suspend fun getTargets(): List<String> =
        api.group().allGroups.await().map { it.name } + api.persistentServer().allPersistentServers.await().map { it.name }

    private suspend fun getNumericalIds(groupName: String): List<Int> =
        api.server().getServersByGroup(groupName).await().map { it.numericalId }

    private fun suggestTargets(): CompletableFuture<List<Suggestion>> =
        scope.future { getTargets().map { Suggestion.suggestion(it) } }

    private fun suggestStates(): CompletableFuture<List<Suggestion>> =
        CompletableFuture.completedFuture(config.get().joinstates.map { Suggestion.suggestion(it.name) })

    private fun suggestNumericalIds(context: CommandContext<C>): CompletableFuture<List<Suggestion>> {
        val groupName = getGroupArgument(context) ?: return CompletableFuture.completedFuture(emptyList())
        return scope.future { getNumericalIds(groupName).map { Suggestion.suggestion(it.toString()) } }
    }

    private fun suggestIdsAndStates(context: CommandContext<C>): CompletableFuture<List<Suggestion>> {
        val states = config.get().joinstates.map { Suggestion.suggestion(it.name) }
        val groupName = getGroupArgument(context) ?: return CompletableFuture.completedFuture(states)
        return scope.future { getNumericalIds(groupName).map { Suggestion.suggestion(it.toString()) } + states }
    }

    private fun getGroupArgument(context: CommandContext<C>): String? =
        context.rawInput().input().split(" ").filter { it.isNotBlank() }.getOrNull(3)
}
