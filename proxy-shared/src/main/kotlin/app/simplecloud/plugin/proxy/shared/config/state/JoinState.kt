package app.simplecloud.plugin.proxy.shared.config.state

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class JoinState(
    val name: String = "",
    val permission: JoinStatePermission = JoinStatePermission(),
    @Setting("forced-motd-layout") val forcedMotdLayout: String? = null
)
