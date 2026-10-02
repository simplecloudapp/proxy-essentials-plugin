package app.simplecloud.plugin.proxy.shared.config

import app.simplecloud.plugin.api.shared.config.AbstractMessageConfig
import app.simplecloud.plugin.api.shared.config.VersionedConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class MessageConfig(
    override val version: Int = ConfigVersion.VERSION,
    override val variables: Map<String, String> = mapOf("prefix" to "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>"),
    val format: FormatConfig = FormatConfig(),
    val kick: KickMessages = KickMessages(),
    val command: CommandMessages = CommandMessages()
) : VersionedConfig, AbstractMessageConfig()

@ConfigSerializable
data class FormatConfig(
    val date: String = "dd.MM.yyyy",
    val time: String = "HH:mm:ss",
    val pingColors: List<PingColor> = listOf(
        PingColor(0, "<green>"),
        PingColor(50, "<yellow>"),
        PingColor(100, "<gold>"),
        PingColor(150, "<red>"),
        PingColor(200, "<dark_red>")
    )
)

@ConfigSerializable
data class PingColor(
    val ping: Int = 0,
    val color: String = ""
)

@ConfigSerializable
data class KickMessages(
    val noPermission: String = "<red>The network is currently in maintenance mode. Please try again later.",
    val networkFull: String = "<red>The network is currently full. Please try again later.",
    val noJoinState: String = "<red>No join state found for server. Please try again later."
)

@ConfigSerializable
data class CommandMessages(
    val help: ListMessages = ListMessages(
        header = "<prefix>Available /scproxy commands:",
        entry = "   <color:#a3a3a3><command>"
    ),
    val failure: String = "<prefix><color:#ff0000>The command failed, check the console for details.",
    val reload: ReloadMessages = ReloadMessages(),
    val joinState: JoinStateMessages = JoinStateMessages(),
    val layout: LayoutMessages = LayoutMessages()
)

@ConfigSerializable
data class ReloadMessages(
    val start: String = "<prefix>Reloading ProxyEssentials configurations...",
    val success: String = "<prefix>Successfully reloaded all ProxyEssentials configurations.",
    val failure: String = "<prefix>Failed to reload configurations: <color:#ff0000><error>"
)

@ConfigSerializable
data class JoinStateMessages(
    val server: UpdateMessages = UpdateMessages(
        updateSuccess = "<prefix>Join state of server updated successfully.",
        updateFailure = "<prefix>Failed to update join state of server.",
        updateNoChange = "<prefix>Join state of server did not change."
    ),
    val group: UpdateMessages = UpdateMessages(
        updateSuccess = "<prefix>Join state of group updated successfully.",
        updateFailure = "<prefix>Failed to update join state of group.",
        updateNoChange = "<prefix>Join state of group did not change."
    ),
    val help: ListMessages = ListMessages(
        header = "<prefix>Commands of join state:",
        entry = "   <color:#a3a3a3><command>"
    ),
    val list: JoinStateListMessages = JoinStateListMessages()
)

@ConfigSerializable
data class JoinStateListMessages(
    val groups: ListMessages = ListMessages(
        header = "<prefix>Groups with their join states:",
        entry = "   <color:#a3a3a3><group> <color:#ffffff>- <color:#a3a3a3><state>"
    ),
    val states: ListMessages = ListMessages(
        header = "<prefix>Available join states:",
        entry = "   <color:#a3a3a3><state> <color:#ffffff>- <color:#a3a3a3><joinPermission>"
    )
)

@ConfigSerializable
data class LayoutMessages(
    val help: ListMessages = ListMessages(
        header = "<prefix>Commands of layout:",
        entry = "   <color:#a3a3a3><command>"
    ),
    val info: ListMessages = ListMessages(
        header = "<prefix>Layout for <color:#a3a3a3><group><color:#ffffff>:",
        entry = "   <color:#a3a3a3><layout>"
    ),
    val set: UpdateMessages = UpdateMessages(
        updateSuccess = "<prefix>Layout for group updated successfully.",
        updateFailure = "<prefix>Failed to update layout for group.",
        updateNoChange = "<prefix>Layout for group did not change."
    )
)

@ConfigSerializable
data class ListMessages(
    val header: String = "",
    val entry: String = ""
)

@ConfigSerializable
data class UpdateMessages(
    val updateSuccess: String = "<prefix>Updated successfully.",
    val updateFailure: String = "<prefix>Failed to update.",
    val updateNoChange: String = "<prefix>Nothing changed."
)
