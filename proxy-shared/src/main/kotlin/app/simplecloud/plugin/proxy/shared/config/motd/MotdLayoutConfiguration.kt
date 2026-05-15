package app.simplecloud.plugin.proxy.shared.config.motd

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class MotdEntry(
    val name: String = "default",
    val line1: String = "<color:#0ea5e9>A simplecloud.app network",
    val line2: String = " "
)

@ConfigSerializable
data class MotdConfig(
    val enabled: Boolean = true,
    @Setting("update-type") val updateType: MotdUpdateType = MotdUpdateType.RANDOM,
    @Setting("update-time") val updateTime: Int? = null,
    val layouts: List<MotdEntry> = listOf(MotdEntry())
)

@ConfigSerializable
data class ServerIconConfig(
    val enabled: Boolean = true,
    val file: String = "simplecloud.png"
)

@ConfigSerializable
data class PlayerListConfig(
    val enabled: Boolean = true,
    @Setting("player-list") val entries: List<String> = listOf("", "simplecloud.app", "")
)

@ConfigSerializable
data class VersionNameConfig(
    val enabled: Boolean = false,
    val text: String = "Example"
)

@ConfigSerializable
data class SlotsConfig(
    val enabled: Boolean = true,
    val type: MaxPlayerDisplayType = MaxPlayerDisplayType.REAL,
    @Setting("fake-slots") val fakeSlots: Int = 100,
    @Setting("dynamic-player-range") val dynamicPlayerRange: Int = 5
)

@ConfigSerializable
data class VersionConfig(
    val name: VersionNameConfig = VersionNameConfig(),
    val slots: SlotsConfig = SlotsConfig()
)

@ConfigSerializable
data class MotdLayoutConfiguration(
    @Setting("config-version") val configVersion: String = "2",
    val motd: MotdConfig = MotdConfig(),
    @Setting("server-icon") val serverIcon: ServerIconConfig = ServerIconConfig(),
    @Setting("player-list") val playerList: PlayerListConfig = PlayerListConfig(),
    @Setting("version") val versionSettings: VersionConfig = VersionConfig()
)
