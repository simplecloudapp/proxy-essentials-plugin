package app.simplecloud.plugin.proxy.shared.config.state

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class JoinStateConfiguration(
    val version: String = "1",
    val defaultState: String = "public",
    val joinStates: List<JoinState> = listOf(
        JoinState("public", "", "simplecloud.join.full.public", "public"),
        JoinState("premium", "simplecloud.join.premium", "simplecloud.join.full.premium", "premium"),
        JoinState("maintenance", "simplecloud.join.maintenance", "simplecloud.join.full.maintenance", "maintenance")
    )
)