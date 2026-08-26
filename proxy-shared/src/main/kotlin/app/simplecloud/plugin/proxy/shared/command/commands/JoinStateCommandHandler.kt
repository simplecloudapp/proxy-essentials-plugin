package app.simplecloud.plugin.proxy.shared.command.commands

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import app.simplecloud.plugin.proxy.shared.command.ProxyCommandSender
import app.simplecloud.plugin.proxy.shared.config.MessageConfig
import app.simplecloud.plugin.proxy.shared.utilities.ProxyPermissions
import kotlinx.coroutines.*
import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.parser.standard.IntegerParser
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.suggestion.Suggestion
import java.util.concurrent.CompletableFuture

class JoinStateCommandHandler<C : ProxyCommandSender>(
    private val manager: CommandManager<C>,
    private val plugin: ProxyPlugin
) {

    private val index = 3
    private val commands = listOf(
        "/scproxy joinstate help",
        "/scproxy joinstate info <group>",
        "/scproxy joinstate info <group> <id>",
        "/scproxy joinstate set <group> <joinstate>",
        "/scproxy joinstate set <group> <id> <joinstate>",
        "<group> can also be the name of a persistent server"
    )

    fun loadCommands() {
        loadHelp()
        loadInfo()
        loadSet()
    }

    private fun loadHelp() {
        val builder = manager.commandBuilder("scproxy").literal("joinstate")

        manager.command(
            builder.permission(ProxyPermissions.JOINSTATE_HELP)
                .handler { context: CommandContext<C> -> handleHelp(context.sender()) }
                .build()
        )
        manager.command(
            builder.literal("help")
                .permission(ProxyPermissions.JOINSTATE_HELP)
                .handler { context: CommandContext<C> -> handleHelp(context.sender()) }
                .build()
        )
    }

    private fun handleHelp(sender: ProxyCommandSender) {
        val messages = plugin.messageConfig.get()
        val entry = messages.command.joinState.help.entry

        sender.sendMessage(messages.resolve(messages.command.joinState.help.header))
        commands.forEach { command ->
            sender.sendMessage(entry.replace("<command>", command))
        }
    }

    private fun loadInfo() {
        manager.command(
            manager.commandBuilder("scproxy")
                .literal("joinstate")
                .literal("info")
                .required("group", StringParser.stringParser()) { _, _ -> suggestGroups() }
                .optional("id", IntegerParser.integerParser(1)) { context, _ -> suggestNumericalIds(context) }
                .permission(ProxyPermissions.JOINSTATE_INFO)
                .handler { context: CommandContext<C> ->
                    CoroutineScope(Dispatchers.IO).launch { handleInfo(context) }
                }
                .build()
        )
    }

    private suspend fun handleInfo(context: CommandContext<C>) {
        val group = context.get<String>("group")
        val id = context.optional<Int>("id")
        val messages = plugin.messageConfig.get()

        val name = when {
            id.isPresent -> "$group-${id.get()}"
            else -> group
        }
        val state = when {
            id.isPresent -> plugin.joinStateService.getJoinStateAtService(group, id.get())
            else -> joinStateOf(group)
        }

        context.sender().sendMessage(
            messages.resolve(messages.command.joinState.list.groups.entry)
                .replace("<group>", name)
                .replace("<state>", state)
        )
    }

    private fun loadSet() {
        manager.command(
            manager.commandBuilder("scproxy")
                .literal("joinstate")
                .literal("set")
                .required("group", StringParser.stringParser()) { _, _ -> suggestGroups() }
                .required("idOrJoinstate", StringParser.stringParser()) { context, _ ->
                    suggestIdsAndJoinStates(context)
                }
                .optional("joinstate", StringParser.stringParser()) { _, _ -> suggestJoinStates() }
                .permission(ProxyPermissions.JOINSTATE_SET)
                .handler { context: CommandContext<C> ->
                    CoroutineScope(Dispatchers.IO).launch { handleSet(context) }
                }
                .build()
        )
    }

    private suspend fun handleSet(context: CommandContext<C>) {
        val group = context.get<String>("group")
        val idOrJoinstate = context.get<String>("idOrJoinstate")
        val joinstate = context.optional<String>("joinstate")
        val messages = plugin.messageConfig.get()

        if (!joinstate.isPresent) {
            handleSetGroup(context.sender(), group, idOrJoinstate, messages)
            return
        }

        val numericalId = idOrJoinstate.toIntOrNull()
        if (numericalId == null) {
            context.sender().sendMessage(messages.resolve(messages.command.joinState.server.updateFailure))
            return
        }

        handleSetService(context.sender(), group, numericalId, joinstate.get(), messages)
    }

    private suspend fun handleSetGroup(
        sender: ProxyCommandSender,
        group: String,
        state: String,
        messages: MessageConfig
    ) {
        if (plugin.joinStateResolver.resolveJoinState(state) == null) {
            sender.sendMessage(messages.resolve(messages.command.joinState.group.updateFailure))
            return
        }

        if (!plugin.cloudControllerHandler.groupExists(group)) {
            handleSetPersistentServer(sender, group, state, messages)
            return
        }

        if (plugin.joinStateService.getJoinStateAtGroup(group) == state) {
            sender.sendMessage(messages.resolve(messages.command.joinState.group.updateNoChange))
            return
        }

        val updated = plugin.joinStateService.setJoinStateAtGroupAndAllServicesInGroup(group, state)
        sender.sendMessage(messages.resolve(updateResult(messages, updated, isGroup = true)))
    }

    private suspend fun handleSetPersistentServer(
        sender: ProxyCommandSender,
        serverName: String,
        state: String,
        messages: MessageConfig
    ) {
        if (plugin.joinStateService.getJoinStateAtPersistentServer(serverName) == state) {
            sender.sendMessage(messages.resolve(messages.command.joinState.server.updateNoChange))
            return
        }

        val updated = plugin.joinStateService.setJoinStateAtPersistentServer(serverName, state)
        sender.sendMessage(messages.resolve(updateResult(messages, updated, isGroup = false)))
    }

    private suspend fun handleSetService(
        sender: ProxyCommandSender,
        group: String,
        numericalId: Int,
        state: String,
        messages: MessageConfig
    ) {
        if (plugin.joinStateResolver.resolveJoinState(state) == null) {
            sender.sendMessage(messages.resolve(messages.command.joinState.server.updateFailure))
            return
        }

        if (plugin.joinStateService.getJoinStateAtService(group, numericalId) == state) {
            sender.sendMessage(messages.resolve(messages.command.joinState.server.updateNoChange))
            return
        }

        val updated = plugin.joinStateService.setJoinStateAtService(group, numericalId, state)
        if (updated && isCurrentService(group, numericalId)) {
            plugin.joinStateService.localState = state
        }

        sender.sendMessage(messages.resolve(updateResult(messages, updated, isGroup = false)))
    }

    private fun isCurrentService(group: String, numericalId: Int): Boolean {
        val currentServer = plugin.cloudControllerHandler.currentServer ?: return false
        if (!currentServer.isFromGroup) return false

        return currentServer.group?.name == group && currentServer.numericalId == numericalId
    }

    private fun updateResult(messages: MessageConfig, updated: Boolean, isGroup: Boolean): String {
        val updateMessages = when {
            isGroup -> messages.command.joinState.group
            else -> messages.command.joinState.server
        }

        return when {
            updated -> updateMessages.updateSuccess
            else -> updateMessages.updateFailure
        }
    }

    private suspend fun joinStateOf(target: String): String {
        return when {
            plugin.cloudControllerHandler.groupExists(target) ->
                plugin.joinStateService.getJoinStateAtGroup(target)

            else -> plugin.joinStateService.getJoinStateAtPersistentServer(target)
        }
    }

    private fun suggestGroups(): CompletableFuture<List<Suggestion>> = runBlocking {
        val targets = plugin.cloudControllerHandler.getAllGroups() +
            plugin.cloudControllerHandler.getAllPersistentServerNames()

        CompletableFuture.completedFuture(targets.map { Suggestion.suggestion(it) })
    }

    private fun suggestJoinStates(): CompletableFuture<List<Suggestion>> {
        val states = plugin.config.get().joinstates
            .map { Suggestion.suggestion(it.name) }

        return CompletableFuture.completedFuture(states)
    }

    private fun suggestNumericalIds(context: CommandContext<C>): CompletableFuture<List<Suggestion>> = runBlocking {
        val group = groupToken(context)
            ?: return@runBlocking CompletableFuture.completedFuture(emptyList<Suggestion>())

        val ids = plugin.cloudControllerHandler.getAllNumericalIdsFromGroup(group)
            .map { Suggestion.suggestion(it.toString()) }

        CompletableFuture.completedFuture(ids)
    }

    private fun suggestIdsAndJoinStates(context: CommandContext<C>): CompletableFuture<List<Suggestion>> {
        val stateSuggestions = plugin.config.get().joinstates
            .map { Suggestion.suggestion(it.name) }
        val group = groupToken(context) ?: return CompletableFuture.completedFuture(stateSuggestions)

        return runBlocking {
            val idSuggestions = plugin.cloudControllerHandler.getAllNumericalIdsFromGroup(group)
                .map { Suggestion.suggestion(it.toString()) }

            CompletableFuture.completedFuture(idSuggestions + stateSuggestions)
        }
    }

    private fun groupToken(context: CommandContext<C>): String? {
        return context.rawInput().input()
            .split(" ")
            .filter { it.isNotBlank() }
            .getOrNull(index)
    }
}
