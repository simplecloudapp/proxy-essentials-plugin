package app.simplecloud.plugin.proxy.shared.config.placeholder

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class PingColor(
    val ping: Int,
    val color: String
)
