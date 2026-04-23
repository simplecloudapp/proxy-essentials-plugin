package app.simplecloud.plugin.proxy.shared.config.tablis

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class TabListGroup(
    val name: String = "*",
    val layout: List<TabList> = listOf(TabList()),
    @Setting("update-time") val updateTime: Long = 20L,
)
