package app.simplecloud.plugin.proxy.shared.tablist

import app.simplecloud.plugin.api.shared.config.ConfigurationFactory
import app.simplecloud.plugin.proxy.shared.config.ProxyEssentialsConfig
import app.simplecloud.plugin.proxy.shared.config.TabListEntry

class TabListService(
    private val config: ConfigurationFactory<ProxyEssentialsConfig>
) {

    fun getTabList(serverName: String): TabListEntry? {
        val groups = config.get().tablist
        val group = groups.find { it.name.equals(serverName, ignoreCase = true) }
            ?: groups.find { serverName.startsWith(it.name, ignoreCase = true) }
            ?: groups.find { it.name == "*" }
            ?: groups.find { it.name.equals("global", ignoreCase = true) }
            ?: return null
        if (group.layout.isEmpty()) return null
        if (group.updateTime <= 0) return group.layout.first()

        val timeSlot = System.currentTimeMillis() / (group.updateTime * 50L)
        return group.layout[timeSlot.mod(group.layout.size)]
    }

    fun getUpdateIntervalMillis(): Long? {
        val updateTime = config.get().tablist.map { it.updateTime }.filter { it > 0 }.minOrNull() ?: return null
        return updateTime * 50L
    }

}
