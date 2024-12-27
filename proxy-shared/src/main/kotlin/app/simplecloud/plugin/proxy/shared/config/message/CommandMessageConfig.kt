package app.simplecloud.plugin.proxy.shared.config.message

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class CommandMessageConfig(
    val joinStateServerUpdateSuccess: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Join state of server updated successfully.",
    val joinStateServerUpdateFailure: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Failed to update join state of server.",
    val joinStateServerUpdateNoChange: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Join state of server did not change.",
    val joinStateGroupUpdateSuccess: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Join state of group updated successfully.",
    val joinStateGroupUpdateFailure: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Failed to update join state of group.",
    val joinStateGroupUpdateNoChange: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Join state of group did not change.",
    val joinStateHelpHeader: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Commands of join state:",
    val joinStateHelpCommand: String = "   <color:#a3a3a3><command>",
    val joinStateGroupListHeader: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Groups with every join states:",
    val joinStateGroupListEntry: String = "   <color:#a3a3a3><group> <color:#ffffff>- <color:#a3a3a3><state>",
    val joinStateStateListHeader: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>List of every join states:",
    val joinStateStateListEntry: String = "   <color:#a3a3a3><state> <color:#ffffff>- <color:#a3a3a3><joinPermission>"
)