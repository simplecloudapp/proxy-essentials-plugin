package app.simplecloud.plugin.proxy.shared.config.message

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

private val PREFIX = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>"

@ConfigSerializable
data class UpdateMessages(
    @Setting("update-success") val updateSuccess: String = "${PREFIX}Updated successfully.",
    @Setting("update-failure") val updateFailure: String = "${PREFIX}Failed to update.",
    @Setting("update-no-change") val updateNoChange: String = "${PREFIX}Nothing changed."
)

@ConfigSerializable
data class ReloadCommandMessages(
    val start: String = "${PREFIX}Reloading ProxyEssentials configurations...",
    val success: String = "${PREFIX}Successfully reloaded all ProxyEssentials configurations.",
    val failure: String = "${PREFIX}Failed to reload configurations: <color:#ff0000><error>"
)

@ConfigSerializable
data class JoinStateHelpMessages(
    val header: String = "${PREFIX}Commands of join state:",
    val entry: String = "   <color:#a3a3a3><command>"
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
data class JoinStateListMessages(
    val groups: JoinStateGroupListMessages = JoinStateGroupListMessages(),
    val states: JoinStateStateListMessages = JoinStateStateListMessages()
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
data class LayoutSetMessages(
    @Setting("update-success") val updateSuccess: String = "${PREFIX}Layout for group updated successfully.",
    @Setting("update-failure") val updateFailure: String = "${PREFIX}Failed to update layout for group.",
    @Setting("update-no-change") val updateNoChange: String = "${PREFIX}Layout for group did not change."
)

@ConfigSerializable
data class LayoutCommandMessages(
    val help: LayoutHelpMessages = LayoutHelpMessages(),
    val info: LayoutInfoMessages = LayoutInfoMessages(),
    val set: LayoutSetMessages = LayoutSetMessages()
)

@ConfigSerializable
data class CommandMessages(
    val reload: ReloadCommandMessages = ReloadCommandMessages(),
    @Setting("join-state") val joinState: JoinStateCommandMessages = JoinStateCommandMessages(),
    val layout: LayoutCommandMessages = LayoutCommandMessages()
)
