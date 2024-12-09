package app.simplecloud.plugin.proxy.shared.handler.command

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
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
        loadJoinStateService()
        loadJoinStateGroup()
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
                    runBlocking {
                        val group = context.get<String>("group")
                        val numericalId = context.get<String>("numericalId")
                        val state = context.get<String>("state")

                        if (proxyPlugin.joinStateHandler.getJoinStateAtService(group, numericalId.toLong()) == state) {
                            context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.joinStateServiceUpdateNoChange)
                            return@runBlocking
                        }

                        val successfully = proxyPlugin.joinStateHandler.setJoinStateAtService(
                            group,
                            numericalId.toLong(),
                            state
                        )

                        if (successfully) {
                            context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.joinStateServiceUpdateSuccess)
                        } else {
                            context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.joinStateServiceUpdateFailure)
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
                    runBlocking {
                        val group = context.get<String>("group")
                        val state = context.get<String>("state")

                        if (proxyPlugin.joinStateHandler.getJoinStateAtGroup(group) == state) {
                            context.sender().sendMessage(proxyPlugin.messagesConfiguration.commandMessage.joinStateGroupUpdateNoChange)
                            return@runBlocking
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