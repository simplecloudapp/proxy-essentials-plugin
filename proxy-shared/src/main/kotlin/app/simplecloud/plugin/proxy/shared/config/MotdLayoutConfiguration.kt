package app.simplecloud.plugin.proxy.shared.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class MotdLayoutConfiguration(
    val configVersion: String = "2",
    val motd: MotdConfig = MotdConfig(),
    val serverIcon: ServerIconConfig = ServerIconConfig(),
    val playerList: PlayerListConfig = PlayerListConfig(),
    val version: VersionConfig = VersionConfig()
)

@ConfigSerializable
data class MotdConfig(
    val enabled: Boolean = true,
    val updateType: MotdUpdateType = MotdUpdateType.RANDOM,
    val updateTime: Int? = null,
    val layouts: List<MotdEntry> = listOf(MotdEntry())
)

@ConfigSerializable
data class MotdEntry(
    val name: String = "default",
    val line1: String = "<color:#0ea5e9>A simplecloud.app network",
    val line2: String = " "
)

enum class MotdUpdateType {
    RANDOM,
    QUEUE
}

@ConfigSerializable
data class ServerIconConfig(
    val enabled: Boolean = true,
    val file: String = "simplecloud.png"
)

@ConfigSerializable
data class PlayerListConfig(
    val enabled: Boolean = true,
    val playerList: List<String> = listOf("", "simplecloud.app", "")
)

@ConfigSerializable
data class VersionConfig(
    val name: VersionNameConfig = VersionNameConfig(),
    val slots: SlotsConfig = SlotsConfig()
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
    val fakeSlots: Int = 100,
    val dynamicPlayerRange: Int = 5
)

enum class MaxPlayerDisplayType {
    REAL,
    FAKE,
    DYNAMIC,
}
