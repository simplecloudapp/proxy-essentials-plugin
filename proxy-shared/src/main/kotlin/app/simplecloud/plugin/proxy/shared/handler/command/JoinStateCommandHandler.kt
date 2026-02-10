package app.simplecloud.plugin.proxy.shared.handler.command

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.suggestion.Suggestion
import java.util.concurrent.CompletableFuture

class JoinStateCommandHandler<C : CommandSender>(
    val commandManager: CommandManager<C>,
    val proxyPlugin: ProxyPlugin
) {

    private fun parseNumericalId(rawValue: String): Int? {
        return rawValue.toIntOrNull()
    }

    fun loadCommands() {
        loadHelp()
        loadJoinStateService()
        loadJoinStateGroup()
        loadJoinStateGroups()
        loadJoinStateStates()
    }

    private fun loadHelp() {
        commandManager.command(
            commandManager.commandBuilder("joinstate")
                .literal("help")
                .permission("simplecloud.command.joinstate.help")
                .handler { context: CommandContext<C> -> handleHelp(context) }
                .build()
        )
        commandManager.command(
            commandManager.commandBuilder("joinstate")
                .permission("simplecloud.command.joinstate.help")
                .handler { context: CommandContext<C> -> handleHelp(context) }
                .build()
        )
    }

    private fun handleHelp(context: CommandContext<C>) {
        context.sender().sendMessage(proxyPlugin.messagesConfiguration.get().commandMessage.joinStateHelpHeader)
        context.sender().sendMessage(proxyPlugin.messagesConfiguration.get().commandMessage.joinStateHelpCommand
            .replace("<command>", "/joinstate server <group> <numericalId> <state>"))
        context.sender().sendMessage(proxyPlugin.messagesConfiguration.get().commandMessage.joinStateHelpCommand
            .replace("<command>", "/joinstate group <group> <state>"))
        context.sender().sendMessage(proxyPlugin.messagesConfiguration.get().commandMessage.joinStateHelpCommand
            .replace("<command>", "/joinstate groups"))
        context.sender().sendMessage(proxyPlugin.messagesConfiguration.get().commandMessage.joinStateHelpCommand
            .replace("<command>", "/joinstate states"))
        context.sender().sendMessage(proxyPlugin.messagesConfiguration.get().commandMessage.joinStateHelpCommand
            .replace("<command>", "/joinstate help"))
    }

    private fun loadJoinStateGroups() {
        commandManager.command(
            commandManager.commandBuilder("joinstate")
                .literal("groups")
                .permission("simplecloud.command.joinstate.groups")
                .handler { context: CommandContext<C> ->
                    context.sender().sendMessage(proxyPlugin.messagesConfiguration.get().commandMessage.joinStateGroupListHeader)
                    CoroutineScope(Dispatchers.IO).launch {
                        proxyPlugin.cloudControllerHandler.getAllGroups().forEach { group ->
                            val state = proxyPlugin.joinStateHandler.getJoinStateAtGroup(group)
                            context.sender().sendMessage(proxyPlugin.messagesConfiguration.get().commandMessage.joinStateGroupListEntry
                                .replace("<group>", group)
                                .replace("<state>", state))
                        }
                    }
                }
                .build()
        )
    }

    private fun loadJoinStateStates() {
        commandManager.command(
            commandManager.commandBuilder("joinstate")
                .literal("states")
                .permission("simplecloud.command.joinstate.states")
                .handler { context: CommandContext<C> ->
                    context.sender().sendMessage(proxyPlugin.messagesConfiguration.get().commandMessage.joinStateStateListHeader)
                    proxyPlugin.joinStateConfiguration.get().joinStates.forEach { state ->
                        context.sender().sendMessage(proxyPlugin.messagesConfiguration.get().commandMessage.joinStateStateListEntry
                            .replace("<state>", state.name)
                            .replace("<joinPermission>", state.joinPermission))
                    }
                }
                .build()
        )
    }

    private fun loadJoinStateService() {
        commandManager.command(
            commandManager.commandBuilder("joinstate")
                .literal("server")
                .required(
                    "group",
                    StringParser.stringParser()
                ) { _, _ ->
                    runBlocking {
                        val suggestionList = proxyPlugin.cloudControllerHandler.getAllGroups().map { Suggestion.suggestion(it) }
                        CompletableFuture.completedFuture(suggestionList)
                    }
                }
                .required(
                    "numericalId",
                    StringParser.stringParser()
                ) { a, _ ->
                    runBlocking {
                        val inputParts = a.rawInput().input().split(" ").filter { it.isNotBlank() }
                        val group = inputParts.getOrNull(2)
                            ?: return@runBlocking CompletableFuture.completedFuture(emptyList<Suggestion>())
                        val suggestionList = proxyPlugin.cloudControllerHandler
                            .getAllNumericalIdsFromGroup(group)
                            .map { Suggestion.suggestion(it.toString()) }
                        CompletableFuture.completedFuture(suggestionList)
                    }
                }
                .required(
                    "state",
                    StringParser.stringParser()
                ) { _, _ ->
                    val suggestionList = proxyPlugin.joinStateConfiguration.get().joinStates.map { Suggestion.suggestion(it.name) }
                    CompletableFuture.completedFuture(suggestionList)
                }
                .permission("simplecloud.command.joinstate.server")
                .handler { context: CommandContext<C> ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val group = context.get<String>("group")
                        val numericalIdRaw = context.get<String>("numericalId")
                        val state = context.get<String>("state")
                        val numericalId = parseNumericalId(numericalIdRaw)
                        if (numericalId == null) {
                            context.sender().sendMessage(proxyPlugin.messagesConfiguration.get().commandMessage.joinStateServerUpdateFailure)
                            return@launch
                        }
                        if (proxyPlugin.joinStateResolver.resolveJoinState(state) == null) {
                            context.sender().sendMessage(proxyPlugin.messagesConfiguration.get().commandMessage.joinStateServerUpdateFailure)
                            return@launch
                        }

                        if (proxyPlugin.joinStateHandler.getJoinStateAtService(group, numericalId) == state) {
                            context.sender().sendMessage(proxyPlugin.messagesConfiguration.get().commandMessage.joinStateServerUpdateNoChange)
                            return@launch
                        }

                        val successfully = proxyPlugin.joinStateHandler.setJoinStateAtService(
                            group,
                            numericalId,
                            state
                        )
                        if (successfully) {
                            val currentServer = proxyPlugin.cloudControllerHandler.currentServer
                            if (currentServer != null && currentServer.isFromGroup) {
                                val currentGroupName = currentServer.group?.name
                                if (currentGroupName == group && currentServer.numericalId == numericalId) {
                                    proxyPlugin.joinStateHandler.localState = state
                                }
                            }
                        }

                        if (successfully) {
                            context.sender().sendMessage(proxyPlugin.messagesConfiguration.get().commandMessage.joinStateServerUpdateSuccess)
                        } else {
                            context.sender().sendMessage(proxyPlugin.messagesConfiguration.get().commandMessage.joinStateServerUpdateFailure)
                        }
                    }
                }
                .build()
        )
    }

    private fun loadJoinStateGroup() {
        commandManager.command(
            commandManager.commandBuilder("joinstate")
                .literal("group")
                .required(
                    "group",
                    StringParser.stringParser()
                ) { _, _ ->
                    runBlocking {
                        val suggestionList = proxyPlugin.cloudControllerHandler.getAllGroups().map { Suggestion.suggestion(it) }
                        CompletableFuture.completedFuture(suggestionList)
                    }
                }
                .required(
                    "state",
                    StringParser.stringParser()
                ) { _, _ ->
                    val suggestionList = proxyPlugin.joinStateConfiguration.get().joinStates.map { Suggestion.suggestion(it.name) }
                    CompletableFuture.completedFuture(suggestionList)
                }
                .permission("simplecloud.command.joinstate.group")
                .handler { context: CommandContext<C> ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val group = context.get<String>("group")
                        val state = context.get<String>("state")
                        if (proxyPlugin.joinStateResolver.resolveJoinState(state) == null) {
                            context.sender().sendMessage(proxyPlugin.messagesConfiguration.get().commandMessage.joinStateGroupUpdateFailure)
                            return@launch
                        }

                        if (proxyPlugin.joinStateHandler.getJoinStateAtGroup(group) == state) {
                            context.sender().sendMessage(proxyPlugin.messagesConfiguration.get().commandMessage.joinStateGroupUpdateNoChange)
                            return@launch
                        }

                        val successfully =
                            proxyPlugin.joinStateHandler.setJoinStateAtGroupAndAllServicesInGroup(
                                group,
                                state
                            )

                        if (successfully) {
                            context.sender().sendMessage(proxyPlugin.messagesConfiguration.get().commandMessage.joinStateGroupUpdateSuccess)
                        } else {
                            context.sender().sendMessage(proxyPlugin.messagesConfiguration.get().commandMessage.joinStateGroupUpdateFailure)
                        }
                    }
                }
                .build()
        )
    }
}
