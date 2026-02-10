package app.simplecloud.plugin.proxy.shared.handler

import app.simplecloud.plugin.proxy.shared.config.tablis.TabListConfiguration
import app.simplecloud.plugin.proxy.shared.config.tablis.TabListGroup

class TabListResolver(
    private val getConfiguration: () -> TabListConfiguration
) {
    private val tabListIndex = mutableMapOf<String, Int>()

    fun incrementIndices() {
        val configuration = getConfiguration()
        configuration.groups.forEach { group ->
            if (group.tabLists.isNotEmpty()) {
                val currentIndex = tabListIndex.getOrPut(group.groupOrService) { 0 }
                tabListIndex[group.groupOrService] = (currentIndex + 1) % group.tabLists.size
            }
        }
    }

    fun findTabListGroup(serviceName: String): TabListGroup? {
        val groups = getConfiguration().groups

        return groups.find { it.groupOrService.equals(serviceName, true) }
            ?: groups.find { serviceName.startsWith(it.groupOrService, true) }
            ?: groups.find { it.groupOrService == "*" }
    }

    fun getCurrentTabList(group: TabListGroup): Pair<String, String>? {
        if (group.tabLists.isEmpty()) return null

        val currentIndex = tabListIndex[group.groupOrService] ?: 0
        val normalizedIndex = currentIndex % group.tabLists.size
        val tabList = group.tabLists[normalizedIndex]

        val header = tabList.header.joinToString("<newline>")
        val footer = tabList.footer.joinToString("<newline>")
        return header to footer
    }
}
