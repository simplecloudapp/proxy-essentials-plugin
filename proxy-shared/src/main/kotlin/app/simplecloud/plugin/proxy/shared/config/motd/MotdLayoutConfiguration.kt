package app.simplecloud.plugin.proxy.shared.config.motd

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class MotdLayoutConfiguration(
    val firstLines: List<String> = listOf("<gradient:#00fcff:#0da1a3><bold>SimpleCloud</bold></gradient> <dark_gray>»<gray> Simplify your network <dark_gray>|<bold><#4595ff> 1.12<#545454> - <#4595ff>1.20</bold>"),
    val secondLines: List<String> = listOf("<dark_gray>× <#178fff>Status<dark_gray>: <bold><#22cc22>Online</bold> <dark_gray>- <#ffffff>%PROXY%"),
    val playerInfo: List<String> = listOf(),
    val versionName: String = "",
    val maxPlayerDisplayType: MaxPlayerDisplayType? = MaxPlayerDisplayType.REAL,
    val dynamicPlayerRange: Int = 1
)