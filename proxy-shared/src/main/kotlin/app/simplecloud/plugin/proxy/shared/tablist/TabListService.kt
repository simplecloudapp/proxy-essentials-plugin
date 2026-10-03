package app.simplecloud.plugin.proxy.shared.tablist

import app.simplecloud.plugin.api.shared.config.ConfigurationFactory
import app.simplecloud.plugin.proxy.shared.config.ProxyEssentialsConfig
import app.simplecloud.plugin.proxy.shared.config.TabListEntry
import app.simplecloud.plugin.proxy.shared.platform.ProxyPlatform
import app.simplecloud.plugin.proxy.shared.platform.ProxyPlayer
import app.simplecloud.plugin.proxy.shared.utilities.text.MessageFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.milliseconds

class TabListService(
    private val platform: ProxyPlatform,
    private val config: ConfigurationFactory<ProxyEssentialsConfig>,
    private val formatter: MessageFormatter,
    private val scope: CoroutineScope
) {

    private val logger = LoggerFactory.getLogger(TabListService::class.java)

    fun start() {
        scope.launch {
            while (isActive) {
                val interval = getUpdateIntervalMillis()
                delay((interval ?: 1000L).milliseconds)
                if (interval == null) continue

                try {
                    platform.getPlayers().forEach { update(it) }
                } catch (e: Exception) {
                    logger.error("Could not update the tablist of the online players", e)
                }
            }
        }
    }

    fun update(player: ProxyPlayer) {
        val serverName = player.getServerName() ?: return
        val tabList = getTabList(serverName) ?: return

        player.sendPlayerListHeaderAndFooter(
            formatter.formatForPlayer(tabList.header, serverName, player.getPing()),
            formatter.formatForPlayer(tabList.footer, serverName, player.getPing())
        )
    }

    private fun getTabList(serverName: String): TabListEntry? {
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

    private fun getUpdateIntervalMillis(): Long? {
        val updateTime = config.get().tablist.map { it.updateTime }.filter { it > 0 }.minOrNull() ?: return null
        return updateTime * 50L
    }

}
