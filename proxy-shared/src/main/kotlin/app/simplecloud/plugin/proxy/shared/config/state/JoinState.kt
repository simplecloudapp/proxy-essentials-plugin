package app.simplecloud.plugin.proxy.shared.config.state

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class JoinState(
    val name: String,
    val joinPermission: String,
    val fullJoinPermission: String,
    val motdLayoutByProxy: String
)
