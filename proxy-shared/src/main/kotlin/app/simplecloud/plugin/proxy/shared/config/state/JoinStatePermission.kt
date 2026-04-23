package app.simplecloud.plugin.proxy.shared.config.state

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class JoinStatePermission(
    val join: String = "",
    val full: String = ""
)
