package app.simplecloud.plugin.proxy.shared.config.tablis

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class TabListGroup(
    var groupOrService: String = "*",
    var tabLists: List<TabList> = listOf(TabList()),
)
