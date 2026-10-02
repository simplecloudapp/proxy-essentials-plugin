package app.simplecloud.plugin.proxy.shared.command

import app.simplecloud.api.CloudApi
import app.simplecloud.plugin.api.shared.config.ConfigurationFactory
import app.simplecloud.plugin.proxy.shared.config.MessageConfig
import app.simplecloud.plugin.proxy.shared.config.ProxyEssentialsConfig
import app.simplecloud.plugin.proxy.shared.joinstate.JoinStateService
import app.simplecloud.plugin.proxy.shared.layout.LayoutRepository
import app.simplecloud.plugin.proxy.shared.layout.LayoutService
import app.simplecloud.plugin.proxy.shared.utilities.CommandPermissions
import app.simplecloud.plugin.proxy.shared.utilities.text.MessageFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.parser.standard.StringParser
import org.incendo.cloud.suggestion.Suggestion
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

class LayoutCommand<C : ProxySender>(
    private val manager: CommandManager<C>,
    private val api: CloudApi,
    private val layoutService: LayoutService,
    private val layoutRepository: LayoutRepository,
    private val joinStateService: JoinStateService,
    private val config: ConfigurationFactory<ProxyEssentialsConfig>,
    private val messages: ConfigurationFactory<MessageConfig>,
    private val formatter: MessageFormatter,
    private val scope: CoroutineScope
) {

    private val logger = LoggerFactory.getLogger(LayoutCommand::class.java)

    private val commands = listOf(
        "/scproxy layout help",
        "/scproxy layout info [group]",
        "/scproxy layout set <group> <layout>"
    )

    fun register() {
        val root = manager.commandBuilder("scproxy").literal("layout")

        manager.command(root.permission(CommandPermissions.LAYOUT_HELP).handler { sendHelp(it.sender()) })
        manager.command(root.literal("help").permission(CommandPermissions.LAYOUT_HELP).handler { sendHelp(it.sender()) })
        manager.command(
            root.literal("info")
                .permission(CommandPermissions.LAYOUT_INFO)
                .handler { sendInfo(it.sender(), "default", config.get().initialLayout) }
        )
        manager.command(
            root.literal("info")
                .required("group", StringParser.stringParser()) { _, _ -> suggestGroups() }
                .permission(CommandPermissions.LAYOUT_INFO)
                .handler { sendGroupInfo(it) }
        )
        manager.command(
            root.literal("set")
                .required("group", StringParser.stringParser()) { _, _ -> suggestGroups() }
                .required("layout", StringParser.stringParser()) { _, _ -> suggestLayouts() }
                .permission(CommandPermissions.LAYOUT_SET)
                .handler { set(it) }
        )
    }

    private fun sendHelp(sender: C) {
        val help = messages.get().command.layout.help
        sender.sendMessage(formatter.format(help.header))
        commands.forEach { sender.sendMessage(formatter.format(help.entry, Placeholder.unparsed("command", it))) }
    }

    private fun sendGroupInfo(context: CommandContext<C>) {
        val groupName = context.get<String>("group")

        runAsync(context.sender(), messages.get().command.failure) {
            val config = config.get()
            val state = joinStateService.getGroupState(groupName)
            val forcedLayout = config.joinstates.find { it.name == state }?.forcedMotdLayout
            val layout = forcedLayout ?: layoutService.getGroupLayout(groupName) ?: config.initialLayout
            sendInfo(context.sender(), groupName, layout)
        }
    }

    private fun sendInfo(sender: C, groupName: String, layout: String) {
        val info = messages.get().command.layout.info
        sender.sendMessage(formatter.format(info.header, Placeholder.unparsed("group", groupName)))
        sender.sendMessage(formatter.format(info.entry, Placeholder.unparsed("layout", layout)))
    }

    private fun set(context: CommandContext<C>) {
        val groupName = context.get<String>("group")
        val layout = context.get<String>("layout")
        val setMessages = messages.get().command.layout.set

        if (layoutRepository.find(layout) == null) {
            context.sender().sendMessage(formatter.format(setMessages.updateFailure))
            return
        }

        runAsync(context.sender(), setMessages.updateFailure) {
            if (layoutService.getGroupLayout(groupName) == layout) {
                context.sender().sendMessage(formatter.format(setMessages.updateNoChange))
                return@runAsync
            }

            layoutService.setGroupLayout(groupName, layout)
            context.sender().sendMessage(formatter.format(setMessages.updateSuccess))
        }
    }

    private fun runAsync(sender: C, failureMessage: String, block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
            } catch (e: Exception) {
                logger.error("The layout command failed", e)
                sender.sendMessage(formatter.format(failureMessage))
            }
        }
    }

    private fun suggestGroups(): CompletableFuture<List<Suggestion>> =
        scope.future { api.group().allGroups.await().map { Suggestion.suggestion(it.name) } }

    private fun suggestLayouts(): CompletableFuture<List<Suggestion>> =
        CompletableFuture.completedFuture(layoutRepository.getNames().map { Suggestion.suggestion(it) })
}
