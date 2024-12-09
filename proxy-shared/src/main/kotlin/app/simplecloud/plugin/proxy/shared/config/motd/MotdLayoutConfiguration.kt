package app.simplecloud.plugin.proxy.shared.config.motd

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class MotdLayoutConfiguration(
    val firstLines: List<String> = listOf("<color:#0ea5e9>A simplecloud.app network"),
    val secondLines: List<String> = listOf(" "),
    val playerInfo: List<String> = listOf(),
    val versionName: String = "",
    var serverIcon: String = "server-icon.png",
    val maxPlayerDisplayType: MaxPlayerDisplayType? = MaxPlayerDisplayType.REAL,
    val dynamicPlayerRange: Int = 1
)