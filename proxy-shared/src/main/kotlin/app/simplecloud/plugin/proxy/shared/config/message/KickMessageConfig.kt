package app.simplecloud.plugin.proxy.shared.config.message

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class KickMessageConfig(
    val networkMaintenance: String = "<red>The network is currently in maintenance mode. Please try again later.",
    val networkFull: String = "<red>The network is currently full. Please try again later.",
)