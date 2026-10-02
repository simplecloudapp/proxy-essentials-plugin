package app.simplecloud.plugin.proxy.shared.config

import app.simplecloud.plugin.api.shared.config.VersionedConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.util.UUID

@ConfigSerializable
data class ProxyEssentialsConfig(
    override val version: Int = ConfigVersion.VERSION,
    val initialState: String = "public",
    val initialLayout: String = "public",
    val showKickReason: Boolean = false,
    val joinstates: List<JoinStateConfig> = listOf(
        JoinStateConfig(
            name = "public",
            permission = JoinStatePermission(full = "simplecloud.proxy-essentials.join.full.public")
        ),
        JoinStateConfig(
            name = "maintenance",
            permission = JoinStatePermission(
                join = "simplecloud.proxy-essentials.join.maintenance",
                full = "simplecloud.proxy-essentials.join.maintenance"
            ),
            forcedMotdLayout = "maintenance"
        )
    ),
    val domains: List<DomainRoute> = emptyList(),
    val whitelist: WhitelistConfig = WhitelistConfig(),
    val playerCount: PlayerCountConfig = PlayerCountConfig(),
    val tablist: List<TabListGroup> = listOf(TabListGroup())
) : VersionedConfig

@ConfigSerializable
data class JoinStateConfig(
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
data class DomainRoute(
    val domain: String = "",
    val target: String = "",
    val rules: List<DomainRule> = emptyList()
)

@ConfigSerializable
data class DomainRule(
    val state: String = "",
    val layout: String = ""
)

@ConfigSerializable
data class WhitelistConfig(
    val enabled: Boolean = false,
    val players: List<String> = emptyList()
) {

    fun contains(name: String, uuid: UUID): Boolean {
        if (!enabled) return false

        val uuidString = uuid.toString()
        return players.any { it.equals(name, ignoreCase = true) || it.equals(uuidString, ignoreCase = true) }
    }
}

@ConfigSerializable
data class PlayerCountConfig(
    val enabled: Boolean = true,
    val additionalGroups: List<String> = emptyList(),
    val additionalPersistentServers: List<String> = emptyList(),
    val updateTime: Long = 20L
)

@ConfigSerializable
data class TabListGroup(
    val name: String = "global",
    val layout: List<TabListEntry> = listOf(TabListEntry()),
    val updateTime: Long = 20L
)

@ConfigSerializable
data class TabListEntry(
    val header: String = "<br><color:#0ea5e9>SimpleCloud v3<br>",
    val footer: String = "<br> <color:#ffffff><online_players> players <color:#cbd5e1>are playing on your network <br> <color:#64748b>  sɪᴍᴘʟᴇᴄʟᴏᴜᴅ.ᴀᴘᴘ<br>"
)
