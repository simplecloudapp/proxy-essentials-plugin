package app.simplecloud.plugin.proxy.shared.config.tablis

import org.spongepowered.configurate.objectmapping.ConfigSerializable


@ConfigSerializable
data class TabListConfiguration(
    val groups: List<TabListGroup> = listOf(
        TabListGroup()
    ),
    val tabListUpdateTime: Long = 1000
) {
}
