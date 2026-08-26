package app.simplecloud.plugin.proxy.velocity.tablist

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import app.simplecloud.plugin.proxy.velocity.ProxyVelocityPlugin
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.scheduler.ScheduledTask
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import kotlin.jvm.optionals.getOrNull

class TabListHandler(
    private val proxyPlugin: ProxyPlugin,
    private val plugin: ProxyVelocityPlugin,
    private val proxyServer: ProxyServer
) {

    private val logger = LoggerFactory.getLogger(TabListHandler::class.java)
    private var task: ScheduledTask? = null

    fun startTabListTask() {
        val updateTimeMillis = proxyPlugin.config.get().tabListUpdateTimeMillis()

        task = proxyServer.scheduler
            .buildTask(plugin, Runnable { updateTabLists() })
            .repeat(updateTimeMillis, TimeUnit.MILLISECONDS)
            .schedule()
    }

    fun stopTabListTask() {
        val runningTask = task
        if (runningTask == null) {
            logger.warn("Can't stop tablist task because it is not initialized")
            return
        }

        runningTask.cancel()
        task = null
    }

    fun updateTabListForPlayer(player: Player) {
        val currentServer = player.currentServer.getOrNull() ?: return
        val tabListGroup = proxyPlugin.tabListResolver.findTabListGroup(currentServer.serverInfo.name) ?: return
        val (header, footer) = proxyPlugin.tabListResolver.getCurrentTabList(tabListGroup) ?: return

        player.sendPlayerListHeaderAndFooter(
            plugin.deserializeToComponent(header, player),
            plugin.deserializeToComponent(footer, player),
        )
    }

    private fun updateTabLists() {
        proxyPlugin.tabListResolver.incrementIndices()
        proxyServer.allPlayers.forEach { updateTabListForPlayer(it) }
    }
}
