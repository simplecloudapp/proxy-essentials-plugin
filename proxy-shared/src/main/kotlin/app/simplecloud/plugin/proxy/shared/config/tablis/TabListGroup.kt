package app.simplecloud.plugin.proxy.shared.config.tablis

data class TabListGroup(
    var groupOrService: String = "*",
    var tabLists: List<TabList> = listOf(TabList()),
    val tabListAnimationDelay: Int = 0
)
