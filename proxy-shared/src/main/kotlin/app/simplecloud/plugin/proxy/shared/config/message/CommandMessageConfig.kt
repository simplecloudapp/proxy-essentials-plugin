package app.simplecloud.plugin.proxy.shared.config.message

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class CommandMessageConfig(
    val toggleMaintenance: String = "<red>You have toggled the maintenance mode.",
    val activateMaintenance: String = "<red>You have activated the maintenance mode.",
    val maintenanceAlreadyActivated: String = "<red>The maintenance mode is already activated.",
    val deactivateMaintenance: String = "<red>You have deactivated the maintenance mode.",
    val maintenanceAlreadyDeactivated: String = "<red>The maintenance mode is already deactivated.",
    val layoutNotFound: String = "<red>The layout could not be found.",
    val layoutMaintenanceAlreadySet: String = "<red>The layout is already set for maintenance.",
    val layoutMaintenanceSet: String = "<red>The layout has been set for maintenance.",
    val layoutNonMaintenanceAlreadySet: String = "<red>The layout is already set for non-maintenance.",
    val layoutNonMaintenanceSet: String = "<red>The layout has been set for non-maintenance."
)