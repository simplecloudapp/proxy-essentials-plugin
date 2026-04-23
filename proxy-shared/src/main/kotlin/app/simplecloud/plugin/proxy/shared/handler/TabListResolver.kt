package app.simplecloud.plugin.proxy.shared.handler

import app.simplecloud.plugin.proxy.shared.config.tablis.TabListGroup

class TabListResolver(
    private val getTabListGroups: () -> List<TabListGroup>
) {
    private val tabListIndex = mutableMapOf<String, Int>()

    fun incrementIndices() {
        getTabListGroups().forEach { group ->
            if (group.layout.isNotEmpty()) {
                val currentIndex = tabListIndex.getOrPut(group.name) { 0 }
                tabListIndex[group.name] = (currentIndex + 1) % group.layout.size
            }
        }
    }

    fun findTabListGroup(serviceName: String): TabListGroup? {
        val groups = getTabListGroups()

        return groups.find { it.name.equals(serviceName, true) }
            ?: groups.find { serviceName.startsWith(it.name, true) }
            ?: groups.find { it.name == "*" }
            ?: groups.find { it.name.equals("global", true) }
    }

    fun getCurrentTabList(group: TabListGroup): Pair<String, String>? {
        if (group.layout.isEmpty()) return null

        val currentIndex = tabListIndex[group.name] ?: 0
        val normalizedIndex = currentIndex % group.layout.size
        val tabList = group.layout[normalizedIndex]

        return tabList.header to tabList.footer
    }
}
