package app.simplecloud.plugin.proxy.shared.config

import app.simplecloud.plugin.proxy.shared.config.domain.DomainMotdRoute
import app.simplecloud.plugin.proxy.shared.config.state.JoinState
import app.simplecloud.plugin.proxy.shared.config.state.JoinStatePermission
import app.simplecloud.plugin.proxy.shared.config.tablis.TabList
import app.simplecloud.plugin.proxy.shared.config.tablis.TabListGroup
import app.simplecloud.plugin.proxy.shared.config.state.WhitelistConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.PostProcess
import org.spongepowered.configurate.objectmapping.meta.Setting
import java.util.logging.Logger

@ConfigSerializable
data class ProxyEssentialsConfig(
    val version: String = "2",
    @Setting("initial-state") val initialState: String = "public",
    @Setting("initial-layout") val initialLayout: String = "public",
    @Setting("show-kick-reason") val showKickReason: Boolean = false,
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
    @Setting("external-server-joinstates") val externalServerJoinStates: Map<String, String> = emptyMap(),
    val domains: List<DomainMotdRoute> = listOf(),
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

    fun playerCountUpdateTimeMillis(): Long? {
        if (!playerCount.enabled || playerCount.updateTime <= 0L) {
            return null
        }

        return playerCount.updateTime * 50L
    }

    @PostProcess
    private fun warnUnknownExternalJoinStates() {
        externalServerJoinStates.forEach { (serverName, stateName) ->
            if (joinstates.none { it.name == stateName }) {
                logger.warning("Unknown join state '$stateName' for external server '$serverName'. Using initial-state '$initialState' instead.")
            }
        }
    }

    companion object {
        private val logger = Logger.getLogger(ProxyEssentialsConfig::class.java.name)
    }
}
