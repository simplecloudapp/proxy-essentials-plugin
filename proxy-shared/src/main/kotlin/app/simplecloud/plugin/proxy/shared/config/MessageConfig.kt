package app.simplecloud.plugin.proxy.shared.config

import app.simplecloud.plugin.api.shared.config.AbstractMessageConfig
import app.simplecloud.plugin.api.shared.config.VersionedConfig
import app.simplecloud.plugin.proxy.shared.utilities.config.ConfigVersion
import app.simplecloud.plugin.proxy.shared.utilities.config.DefaultConfigs
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class MessageConfig(
    override val version: Int = ConfigVersion.VERSION,
    override val variables: Map<String, String> = DefaultConfigs.VARIABLES,
    val kick: KickMessageConfig = KickMessageConfig(),
    val command: CommandMessages = CommandMessages()
) : VersionedConfig, AbstractMessageConfig() {

    fun resolve(message: String): String {
        return variables.entries.fold(message) { resolved, (key, value) ->
            resolved.replace("<$key>", value)
        }
    }
}

@ConfigSerializable
data class KickMessageConfig(
    val noPermission: String = "<red>The network is currently in maintenance mode. Please try again later.",
    val networkFull: String = "<red>The network is currently full. Please try again later.",
    val noJoinState: String = "<red>No join state found for server. Please try again later."
)

@ConfigSerializable
data class CommandMessages(
    val reload: ReloadCommandMessages = ReloadCommandMessages(),
    val joinState: JoinStateCommandMessages = JoinStateCommandMessages(),
    val layout: LayoutCommandMessages = LayoutCommandMessages()
)

@ConfigSerializable
data class ReloadCommandMessages(
    val start: String = "${PREFIX}Reloading ProxyEssentials configurations...",
    val success: String = "${PREFIX}Successfully reloaded all ProxyEssentials configurations.",
    val failure: String = "${PREFIX}Failed to reload configurations: <color:#ff0000><error>"
)

@ConfigSerializable
data class JoinStateCommandMessages(
    val server: UpdateMessages = UpdateMessages(
        "${PREFIX}Join state of server updated successfully.",
        "${PREFIX}Failed to update join state of server.",
        "${PREFIX}Join state of server did not change."
    ),
    val group: UpdateMessages = UpdateMessages(
        "${PREFIX}Join state of group updated successfully.",
        "${PREFIX}Failed to update join state of group.",
        "${PREFIX}Join state of group did not change."
    ),
    val help: JoinStateHelpMessages = JoinStateHelpMessages(),
    val list: JoinStateListMessages = JoinStateListMessages()
)

@ConfigSerializable
data class JoinStateHelpMessages(
    val header: String = "${PREFIX}Commands of join state:",
    val entry: String = "   <color:#a3a3a3><command>"
)

@ConfigSerializable
data class JoinStateListMessages(
    val groups: JoinStateGroupListMessages = JoinStateGroupListMessages(),
    val states: JoinStateStateListMessages = JoinStateStateListMessages()
)

@ConfigSerializable
data class JoinStateGroupListMessages(
    val header: String = "${PREFIX}Groups with their join states:",
    val entry: String = "   <color:#a3a3a3><group> <color:#ffffff>- <color:#a3a3a3><state>"
)

@ConfigSerializable
data class JoinStateStateListMessages(
    val header: String = "${PREFIX}Available join states:",
    val entry: String = "   <color:#a3a3a3><state> <color:#ffffff>- <color:#a3a3a3><joinPermission>"
)

@ConfigSerializable
data class LayoutCommandMessages(
    val help: LayoutHelpMessages = LayoutHelpMessages(),
    val info: LayoutInfoMessages = LayoutInfoMessages(),
    val set: UpdateMessages = UpdateMessages(
        "${PREFIX}Layout for group updated successfully.",
        "${PREFIX}Failed to update layout for group.",
        "${PREFIX}Layout for group did not change."
    )
)

@ConfigSerializable
data class LayoutHelpMessages(
    val header: String = "${PREFIX}Commands of layout:",
    val entry: String = "   <color:#a3a3a3><command>"
)

@ConfigSerializable
data class LayoutInfoMessages(
    val header: String = "${PREFIX}Layout for <color:#a3a3a3><group><color:#ffffff>:",
    val entry: String = "   <color:#a3a3a3><layout>"
)

@ConfigSerializable
data class UpdateMessages(
    val updateSuccess: String = "${PREFIX}Updated successfully.",
    val updateFailure: String = "${PREFIX}Failed to update.",
    val updateNoChange: String = "${PREFIX}Nothing changed."
)

const val PREFIX = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>"