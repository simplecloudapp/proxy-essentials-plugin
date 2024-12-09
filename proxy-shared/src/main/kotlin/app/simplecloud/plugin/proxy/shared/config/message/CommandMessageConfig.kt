package app.simplecloud.plugin.proxy.shared.config.message

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class CommandMessageConfig(
    val joinStateServiceUpdateSuccess: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Join state of service updated successfully.",
    val joinStateServiceUpdateFailure: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Failed to update join state of service.",
    val joinStateServiceUpdateNoChange: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Join state of service did not change.",
    val joinStateGroupUpdateSuccess: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Join state of group updated successfully.",
    val joinStateGroupUpdateFailure: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Failed to update join state of group.",
    val joinStateGroupUpdateNoChange: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Join state of group did not change.",
    val joinStateHelpHeader: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>Commands of join state:",
    val joinStateHelpCommand: String = "   <color:#a3a3a3><command>",
)