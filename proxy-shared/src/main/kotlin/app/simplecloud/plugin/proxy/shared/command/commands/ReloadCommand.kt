package app.simplecloud.plugin.proxy.shared.command.commands

import app.simplecloud.plugin.proxy.shared.ProxyEssentials
import app.simplecloud.plugin.proxy.shared.command.ProxySender
import app.simplecloud.plugin.proxy.shared.utilities.CommandPermissions
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.incendo.cloud.CommandManager
import org.incendo.cloud.kotlin.coroutines.extension.suspendingHandler
import org.incendo.cloud.permission.Permission
import org.slf4j.LoggerFactory

class ReloadCommand<C : ProxySender>(
    private val plugin: ProxyEssentials,
    private val manager: CommandManager<C>
) {

    private val logger = LoggerFactory.getLogger(ReloadCommand::class.java)

    fun register() {
        manager.command(
            manager.commandBuilder("scproxy")
                .literal("reload")
                .suspendingHandler { context ->
                    val sender = context.sender()
                    val formatter = plugin.messageFormatter
                    sender.sendMessage(formatter.format(plugin.messages.get().command.reload.start))
                    try {
                        plugin.reload()
                        sender.sendMessage(formatter.format(plugin.messages.get().command.reload.success))
                    } catch (e: Exception) {
                        logger.error("Could not reload Proxy Essentials", e)
                        val error = Placeholder.unparsed("error", e.message ?: e.javaClass.simpleName)
                        sender.sendMessage(formatter.format(plugin.messages.get().command.reload.failure, error))
                    }
                }
                .permission(Permission.permission(CommandPermissions.RELOAD))
                .build()
        )
    }
}
