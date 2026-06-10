package app.simplecloud.plugin.proxy.shared.config

import app.simplecloud.plugin.proxy.shared.config.state.JoinState
import app.simplecloud.plugin.proxy.shared.config.state.JoinStatePermission
import app.simplecloud.plugin.proxy.shared.config.tablis.TabList
import app.simplecloud.plugin.proxy.shared.config.tablis.TabListGroup
import app.simplecloud.plugin.proxy.shared.config.state.WhitelistConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class ProxyEssentialsConfig(
    val version: String = "2",
    @Setting("initial-state") val initialState: String = "public",
    @Setting("initial-layout") val initialLayout: String = "public",
    val joinstates: List<JoinState> = listOf(
        JoinState("public", JoinStatePermission("", "simplecloud.proxy-essentials.join.full.public")),
        JoinState(
            "maintenance",
            JoinStatePermission(
                "simplecloud.proxy-essentials.join.maintenance",
                "simplecloud.proxy-essentials.join.maintenance"
            ),
            "maintenance"
        )
    ),
    val whitelist: WhitelistConfig = WhitelistConfig(),
    @Setting("player-count") val playerCount: PlayerCountConfig = PlayerCountConfig(),
    val tablist: List<TabListGroup> = listOf(
        TabListGroup(
            name = "global",
            layout = listOf(TabList()),
            updateTime = 20L
        )
    )
) {
    fun tabListUpdateTimeMillis(): Long {
        val ticks = tablist.minOfOrNull { it.updateTime } ?: 20L
        return ticks * 50L
    }

    fun playerCountUpdateTimeMillis(): Long {
        return playerCount.updateTime.coerceAtLeast(1L) * 50L
    }
}
