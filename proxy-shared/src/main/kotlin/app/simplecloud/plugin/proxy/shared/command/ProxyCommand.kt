package app.simplecloud.plugin.proxy.shared.command

import app.simplecloud.plugin.proxy.shared.ProxyEssentials
import app.simplecloud.plugin.proxy.shared.utilities.CommandPermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.incendo.cloud.CommandManager
import org.slf4j.LoggerFactory

class ProxyCommand<C : ProxySender>(
    private val manager: CommandManager<C>,
    private val essentials: ProxyEssentials,
    private val scope: CoroutineScope
) {

    private val logger = LoggerFactory.getLogger(ProxyCommand::class.java)
    private val formatter = essentials.messageFormatter

    private val commands = listOf(
        "/scproxy help",
        "/scproxy reload",
        "/scproxy joinstate help",
        "/scproxy joinstate list",
        "/scproxy joinstate info [group]",
        "/scproxy joinstate info <group> <id>",
        "/scproxy joinstate set <group> <joinstate>",
        "/scproxy joinstate set <group> <id> <joinstate>",
        "/scproxy layout help",
        "/scproxy layout info [group]",
        "/scproxy layout set <group> <layout>"
    )

    fun register() {
        val root = manager.commandBuilder("scproxy")

        manager.command(root.permission(CommandPermissions.HELP).handler { sendHelp(it.sender()) })
        manager.command(root.literal("help").permission(CommandPermissions.HELP).handler { sendHelp(it.sender()) })
        manager.command(root.literal("reload").permission(CommandPermissions.RELOAD).handler { reload(it.sender()) })
    }

    private fun sendHelp(sender: C) {
        val help = essentials.messages.get().command.help
        sender.sendMessage(formatter.format(help.header))
        commands.forEach { sender.sendMessage(formatter.format(help.entry, Placeholder.unparsed("command", it))) }
    }

    private fun reload(sender: C) {
        sender.sendMessage(formatter.format(essentials.messages.get().command.reload.start))

        scope.launch {
            try {
                essentials.reload()
                sender.sendMessage(formatter.format(essentials.messages.get().command.reload.success))
            } catch (e: Exception) {
                logger.error("Could not reload Proxy Essentials", e)
                val error = Placeholder.unparsed("error", e.message ?: e.javaClass.simpleName)
                sender.sendMessage(formatter.format(essentials.messages.get().command.reload.failure, error))
            }
        }
    }
}
