package app.simplecloud.plugin.proxy.shared.config.message

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class CommandMessageConfig(
    val joinStateServiceUpdateSuccess: String = "Join state of service updated successfully.",
    val joinStateServiceUpdateFailure: String = "Failed to update join state of service.",
    val joinStateServiceUpdateNoChange: String = "Join state of service did not change.",
    val joinStateGroupUpdateSuccess: String = "Join state of group updated successfully.",
    val joinStateGroupUpdateFailure: String = "Failed to update join state of group.",
    val joinStateGroupUpdateNoChange: String = "Join state of group did not change."
)