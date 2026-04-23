package app.simplecloud.plugin.proxy.shared.config.message

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class KickMessageConfig(
    @Setting("no-permission") val noPermission: String = "<red>The network is currently in maintenance mode. Please try again later.",
    @Setting("network-full") val networkFull: String = "<red>The network is currently full. Please try again later.",
    @Setting("no-join-state") val noJoinState: String = "<red>No join state found for server. Please try again later."
)