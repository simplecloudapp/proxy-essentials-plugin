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
        context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.joinStateHelpHeader)
        context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.joinStateHelpCommand
            .replace("<command>", "/joinstate server <group> <numericalId> <state>"))
        context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.joinStateHelpCommand
            .replace("<command>", "/joinstate group <group> <state>"))
        context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.joinStateHelpCommand
            .replace("<command>", "/joinstate groups"))
        context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.joinStateHelpCommand
            .replace("<command>", "/joinstate states"))
        context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.joinStateHelpCommand
            .replace("<command>", "/joinstate help"))
    }

    private fun loadJoinStateGroups() {
        commandManager.command(
            commandManager.commandBuilder("joinstate")
                .literal("groups")
                .permission("simplecloud.command.joinstate.groups")
                .handler { context: CommandContext<C> ->
                    context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.joinStateGroupListHeader)
                    CoroutineScope(Dispatchers.IO).launch {
                        proxyPlugin.cloudControllerHandler.getAllGroups().forEach { group ->
                            val state = proxyPlugin.joinStateHandler.getJoinStateAtGroup(group)
                            context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.joinStateGroupListEntry
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
                    context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.joinStateStateListHeader)
                    proxyPlugin.joinStateConfiguration.joinStates.forEach { state ->
                        context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.joinStateStateListEntry
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
                        val group = a.rawInput().input().split(" ")[2]
                        val suggestionList = proxyPlugin.cloudControllerHandler.getAllNumericalIdsFromGroup(group).map { Suggestion.suggestion(it.toString()) }
                        CompletableFuture.completedFuture(suggestionList)
                    }
                }
                .required(
                    "state",
                    StringParser.stringParser()
                ) { _, _ ->
                    val suggestionList = proxyPlugin.joinStateConfiguration.joinStates.map { Suggestion.suggestion(it.name) }
                    CompletableFuture.completedFuture(suggestionList)
                }
                .permission("simplecloud.command.joinstate.server")
                .handler { context: CommandContext<C> ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val group = context.get<String>("group")
                        val numericalId = context.get<String>("numericalId")
                        val state = context.get<String>("state")

                        if (proxyPlugin.joinStateHandler.getJoinStateAtService(group, numericalId.toLong()) == state) {
                            context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.joinStateServerUpdateNoChange)
                            return@launch
                        }

                        proxyPlugin.joinStateHandler.localState = state

                        val successfully = proxyPlugin.joinStateHandler.setJoinStateAtService(
                            group,
                            numericalId.toLong(),
                            state
                        )

                        if (successfully) {
                            context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.joinStateServerUpdateSuccess)
                        } else {
                            context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.joinStateServerUpdateFailure)
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
                    val suggestionList = proxyPlugin.joinStateConfiguration.joinStates.map { Suggestion.suggestion(it.name) }
                    CompletableFuture.completedFuture(suggestionList)
                }
                .permission("simplecloud.command.joinstate.group")
                .handler { context: CommandContext<C> ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val group = context.get<String>("group")
                        val state = context.get<String>("state")

                        if (proxyPlugin.joinStateHandler.getJoinStateAtGroup(group) == state) {
                            context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.joinStateGroupUpdateNoChange)
                            return@launch
                        }

                        val successfully =
                            proxyPlugin.joinStateHandler.setJoinStateAtGroupAndAllServicesInGroup(
                                group,
                                state
                            )

                        if (successfully) {
                            context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.joinStateGroupUpdateSuccess)
                        } else {
                            context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.joinStateGroupUpdateFailure)
                        }
                    }
                }
                .build()
        )
    }
}