package app.simplecloud.plugin.proxy.shared.tablist

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import app.simplecloud.plugin.proxy.shared.config.TabListGroup

class TabListResolver(
    private val plugin: ProxyPlugin
) {

    private val index = mutableMapOf<String, Int>()

    fun incrementIndices() {
        getTabListGroups()
            .filter { it.layout.isNotEmpty() }
            .forEach { group ->
                val currentIndex = index.getOrPut(group.name) { 0 }
                index[group.name] = (currentIndex + 1) % group.layout.size
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

        val currentIndex = index[group.name] ?: 0
        val tabList = group.layout[currentIndex % group.layout.size]

        return tabList.header to tabList.footer
    }

    private fun getTabListGroups(): List<TabListGroup> {
        return plugin.config.get().tablist
    }
}
