package app.simplecloud.plugin.proxy.shared.config.tablis

import org.spongepowered.configurate.objectmapping.ConfigSerializable


@ConfigSerializable
data class TabListConfiguration(
    val version: String = "1",
    val groups: List<TabListGroup> = listOf(
        TabListGroup()
    ),
    val tabListUpdateTime: Long = 3000
) {
}
