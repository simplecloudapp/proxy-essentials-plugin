package app.simplecloud.plugin.proxy.shared.handler.command

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.parser.standard.IntegerParser
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.suggestion.Suggestion
import java.util.concurrent.CompletableFuture

class JoinStateCommandHandler<C : CommandSender>(
    val commandManager: CommandManager<C>,
    val proxyPlugin: ProxyPlugin
) {

    fun loadCommands() {
        loadHelp()
        loadInfo()
        loadSet()
    }

    // ── help ──────────────────────────────────────────────────────────────────

    private fun loadHelp() {
        val builder = commandManager.commandBuilder("scproxy").literal("joinstate")
        commandManager.command(
            builder.permission("simplecloud.proxy-essentials.command.joinstate.help")
                .handler { context: CommandContext<C> -> handleHelp(context) }
                .build()
        )
        commandManager.command(
            builder.literal("help")
                .permission("simplecloud.proxy-essentials.command.joinstate.help")
                .handler { context: CommandContext<C> -> handleHelp(context) }
                .build()
        )
    }

    private fun handleHelp(context: CommandContext<C>) {
        val msgs = proxyPlugin.messagesConfiguration.get()
        val entry = msgs.command.joinState.help.entry
        context.sender().sendMessage(msgs.resolve(msgs.command.joinState.help.header))
        context.sender().sendMessage(entry.replace("<command>", "/scproxy joinstate help"))
        context.sender().sendMessage(entry.replace("<command>", "/scproxy joinstate info <group>"))
        context.sender().sendMessage(entry.replace("<command>", "/scproxy joinstate info <group> <id>"))
        context.sender().sendMessage(entry.replace("<command>", "/scproxy joinstate set <group> <joinstate>"))
        context.sender().sendMessage(entry.replace("<command>", "/scproxy joinstate set <group> <id> <joinstate>"))
    }

    // ── info ──────────────────────────────────────────────────────────────────
    // /scproxy joinstate info <group> [id]

    private fun loadInfo() {
        commandManager.command(
            commandManager.commandBuilder("scproxy")
                .literal("joinstate")
                .literal("info")
                .required("group", StringParser.stringParser()) { _, _ ->
                    runBlocking {
                        proxyPlugin.cloudControllerHandler.getAllGroups()
                            .map { Suggestion.suggestion(it) }
                            .let { CompletableFuture.completedFuture(it) }
                    }
                }
                .optional("id", IntegerParser.integerParser(1)) { context, _ ->
                    runBlocking {
                        val parts = context.rawInput().input().split(" ").filter { it.isNotBlank() }
                        val group = parts.getOrNull(3)
                            ?: return@runBlocking CompletableFuture.completedFuture(emptyList<Suggestion>())
                        proxyPlugin.cloudControllerHandler.getAllNumericalIdsFromGroup(group)
                            .map { Suggestion.suggestion(it.toString()) }
                            .let { CompletableFuture.completedFuture(it) }
                    }
                }
                .permission("simplecloud.proxy-essentials.command.joinstate.info")
                .handler { context: CommandContext<C> ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val group = context.get<String>("group")
                        val optId = context.optional<Int>("id")
                        val msgs = proxyPlugin.messagesConfiguration.get()

                        if (optId.isPresent) {
                            val id = optId.get()
                            val state = proxyPlugin.joinStateHandler.getJoinStateAtService(group, id)
                            context.sender().sendMessage(
                                msgs.resolve(msgs.command.joinState.list.groups.entry)
                                    .replace("<group>", "$group-$id")
                                    .replace("<state>", state)
                            )
                        } else {
                            val state = proxyPlugin.joinStateHandler.getJoinStateAtGroup(group)
                            context.sender().sendMessage(
                                msgs.resolve(msgs.command.joinState.list.groups.entry)
                                    .replace("<group>", group)
                                    .replace("<state>", state)
                            )
                        }
                    }
                }
                .build()
        )
    }

    // ── set ───────────────────────────────────────────────────────────────────
    // /scproxy joinstate set <group> <idOrJoinstate> [joinstate]
    //   • 2 args → group mode:   idOrJoinstate = joinstate name
    //   • 3 args → service mode: idOrJoinstate = numerical id, joinstate = state name

    private fun loadSet() {
        commandManager.command(
            commandManager.commandBuilder("scproxy")
                .literal("joinstate")
                .literal("set")
                .required("group", StringParser.stringParser()) { _, _ ->
                    runBlocking {
                        proxyPlugin.cloudControllerHandler.getAllGroups()
                            .map { Suggestion.suggestion(it) }
                            .let { CompletableFuture.completedFuture(it) }
                    }
                }
                .required("idOrJoinstate", StringParser.stringParser()) { context, _ ->
                    val parts = context.rawInput().input().split(" ").filter { it.isNotBlank() }
                    // if we already have a group token, suggest IDs first, then states
                    val group = parts.getOrNull(3)
                    val stateSuggestions = proxyPlugin.proxyEssentialsConfig.get().joinstates
                        .map { Suggestion.suggestion(it.name) }
                    if (group == null) {
                        CompletableFuture.completedFuture(stateSuggestions)
                    } else {
                        runBlocking {
                            val idSuggestions = proxyPlugin.cloudControllerHandler
                                .getAllNumericalIdsFromGroup(group)
                                .map { Suggestion.suggestion(it.toString()) }
                            CompletableFuture.completedFuture(idSuggestions + stateSuggestions)
                        }
                    }
                }
                .optional("joinstate", StringParser.stringParser()) { _, _ ->
                    val states = proxyPlugin.proxyEssentialsConfig.get().joinstates
                        .map { Suggestion.suggestion(it.name) }
                    CompletableFuture.completedFuture(states)
                }
                .permission("simplecloud.proxy-essentials.command.joinstate.set")
                .handler { context: CommandContext<C> ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val group = context.get<String>("group")
                        val idOrJoinstate = context.get<String>("idOrJoinstate")
                        val optJoinstate = context.optional<String>("joinstate")
                        val msgs = proxyPlugin.messagesConfiguration.get()

                        if (optJoinstate.isPresent) {
                            // service mode: idOrJoinstate must be a valid integer
                            val id = idOrJoinstate.toIntOrNull()
                            if (id == null) {
                                context.sender().sendMessage(msgs.resolve(msgs.command.joinState.server.updateFailure))
                                return@launch
                            }
                            val state = optJoinstate.get()
                            handleSetService(context.sender(), group, id, state, msgs)
                        } else {
                            // group mode: idOrJoinstate is the state name
                            handleSetGroup(context.sender(), group, idOrJoinstate, msgs)
                        }
                    }
                }
                .build()
        )
    }

    private suspend fun handleSetGroup(sender: CommandSender, group: String, state: String, msgs: app.simplecloud.plugin.proxy.shared.config.message.MessageConfig) {
        if (proxyPlugin.joinStateResolver.resolveJoinState(state) == null) {
            sender.sendMessage(msgs.resolve(msgs.command.joinState.group.updateFailure))
            return
        }
        if (proxyPlugin.joinStateHandler.getJoinStateAtGroup(group) == state) {
            sender.sendMessage(msgs.resolve(msgs.command.joinState.group.updateNoChange))
            return
        }
        val success = proxyPlugin.joinStateHandler.setJoinStateAtGroupAndAllServicesInGroup(group, state)
        sender.sendMessage(
            if (success) msgs.resolve(msgs.command.joinState.group.updateSuccess)
            else msgs.resolve(msgs.command.joinState.group.updateFailure)
        )
    }

    private suspend fun handleSetService(sender: CommandSender, group: String, id: Int, state: String, msgs: app.simplecloud.plugin.proxy.shared.config.message.MessageConfig) {
        if (proxyPlugin.joinStateResolver.resolveJoinState(state) == null) {
            sender.sendMessage(msgs.resolve(msgs.command.joinState.server.updateFailure))
            return
        }
        if (proxyPlugin.joinStateHandler.getJoinStateAtService(group, id) == state) {
            sender.sendMessage(msgs.resolve(msgs.command.joinState.server.updateNoChange))
            return
        }
        val success = proxyPlugin.joinStateHandler.setJoinStateAtService(group, id, state)
        if (success) {
            val currentServer = proxyPlugin.cloudControllerHandler.currentServer
            if (currentServer != null && currentServer.isFromGroup) {
                val currentGroup = currentServer.group?.name
                if (currentGroup == group && currentServer.numericalId == id) {
                    proxyPlugin.joinStateHandler.localState = state
                }
            }
            sender.sendMessage(msgs.resolve(msgs.command.joinState.server.updateSuccess))
        } else {
            sender.sendMessage(msgs.resolve(msgs.command.joinState.server.updateFailure))
        }
    }
}
