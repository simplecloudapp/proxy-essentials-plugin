package app.simplecloud.plugin.proxy.shared.command

import app.simplecloud.plugin.proxy.shared.ProxyEssentials
import app.simplecloud.plugin.proxy.shared.command.commands.JoinStateCommand
import app.simplecloud.plugin.proxy.shared.command.commands.LayoutCommand
import app.simplecloud.plugin.proxy.shared.command.commands.ReloadCommand
import app.simplecloud.plugin.proxy.shared.utilities.CommandPermissions
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.permission.Permission

class ProxyCommandHandler<C : ProxySender>(
    private val manager: CommandManager<C>,
    private val plugin: ProxyEssentials
) {

    fun register() {
        manager.command(
            manager.commandBuilder("scproxy")
                .handler { context -> sendHelp(context) }
                .permission(Permission.permission(CommandPermissions.HELP))
                .build()
        )
        manager.command(
            manager.commandBuilder("scproxy")
                .literal("help")
                .handler { context -> sendHelp(context) }
                .permission(Permission.permission(CommandPermissions.HELP))
                .build()
        )
        JoinStateCommand(plugin.api, plugin, manager).register()
        LayoutCommand(plugin.api, plugin, manager).register()
        ReloadCommand(plugin, manager).register()
    }

    private fun sendHelp(context: CommandContext<C>) {
        val sender = context.sender()
        val help = plugin.messages.get().command.help
        val formatter = plugin.messageFormatter
        val entries = listOf(
            "/scproxy reload" to CommandPermissions.RELOAD,
            "/scproxy joinstate help" to CommandPermissions.JOINSTATE_HELP,
            "/scproxy joinstate list" to CommandPermissions.JOINSTATE_INFO,
            "/scproxy joinstate info" to CommandPermissions.JOINSTATE_INFO,
            "/scproxy joinstate info <group|ps> <target> [id]" to CommandPermissions.JOINSTATE_INFO,
            "/scproxy joinstate set <group|ps> <target> <joinstate> [id]" to CommandPermissions.JOINSTATE_SET,
            "/scproxy layout help" to CommandPermissions.LAYOUT_HELP,
            "/scproxy layout info [group]" to CommandPermissions.LAYOUT_INFO,
            "/scproxy layout set <group> <layout>" to CommandPermissions.LAYOUT_SET
        ).filter { (_, permission) -> sender.hasPermission(permission) }

        sender.sendMessage(formatter.format(help.header))
        entries.forEach { (command, _) ->
            sender.sendMessage(formatter.format(help.entry, Placeholder.unparsed("command", command)))
        }
    }
}
