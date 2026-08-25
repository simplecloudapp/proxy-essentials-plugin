package app.simplecloud.plugin.proxy.shared.config

import app.simplecloud.plugin.api.shared.config.VersionedConfig
import app.simplecloud.plugin.proxy.shared.utilities.config.ConfigVersion
import app.simplecloud.plugin.proxy.shared.utilities.config.DefaultConfigs
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class ProxyEssentialsConfig(
    override val version: Int = ConfigVersion.VERSION,
    val initialState: String = "public",
    val initialLayout: String = "public",
    val showKickReason: Boolean = false,
    val joinstates: List<JoinState> = DefaultConfigs.JOIN_STATES,
    val domains: List<DomainMotdRoute> = listOf(),
    val whitelist: WhitelistConfig = WhitelistConfig(),
    val playerCount: PlayerCountConfig = PlayerCountConfig(),
    val tablist: List<TabListGroup> = DefaultConfigs.TABLIST_GROUPS
) : VersionedConfig {
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
}

@ConfigSerializable
data class JoinState(
    val name: String = "",
    val permission: JoinStatePermission = JoinStatePermission(),
    val forcedMotdLayout: String? = null
)

@ConfigSerializable
data class JoinStatePermission(
    val join: String = "",
    val full: String = ""
)

@ConfigSerializable
data class DomainMotdRoute(
    val domain: String = "",
    val target: String = "",
    val rules: List<DomainMotdRule> = listOf()
)

@ConfigSerializable
data class DomainMotdRule(
    val state: String = "",
    val layout: String = ""
)

@ConfigSerializable
data class WhitelistConfig(
    val enabled: Boolean = true,
    val players: List<String> = listOf("Notch")
)

@ConfigSerializable
data class PlayerCountConfig(
    val enabled: Boolean = true,
    val additionalGroups: List<String> = emptyList(),
    val additionalPersistentServers: List<String> = emptyList(),
    val updateTime: Long = 20L
)

@ConfigSerializable
data class TabListGroup(
    val name: String = "*",
    val layout: List<TabList> = listOf(TabList()),
    val updateTime: Long = 20L,
)

@ConfigSerializable
data class TabList(
    val header: String = "<br><color:#0ea5e9>SimpleCloud v3<br>",
    val footer: String = "<br> <color:#ffffff><online_players> players <color:#cbd5e1>are playing on your network <br> <color:#64748b>  sɪᴍᴘʟᴇᴄʟᴏᴜᴅ.ᴀᴘᴘ<br>",
)
