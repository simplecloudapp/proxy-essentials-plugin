package app.simplecloud.plugin.proxy.shared.config.state

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class JoinStateConfiguration(
    val defaultState: String = "public",
    val joinStates: List<JoinState> = listOf(
        JoinState("public", "", "simplecloud.join.full.public", "public"),
        JoinState("closedBeta", "simplecloud.join.closedbeta", "simplecloud.join.full.closedbeta", "closed_beta"),
        JoinState("maintenance", "simplecloud.join.maintenance", "simplecloud.join.full.maintenance", "maintenance")
    )
)