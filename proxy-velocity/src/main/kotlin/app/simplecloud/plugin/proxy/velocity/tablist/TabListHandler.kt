package app.simplecloud.plugin.proxy.velocity.tablist

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import app.simplecloud.plugin.proxy.velocity.ProxyVelocityPlugin
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.scheduler.ScheduledTask
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import kotlin.jvm.optionals.getOrNull

class TabListHandler(
    private val proxyPlugin: ProxyPlugin,
    private val plugin: ProxyVelocityPlugin,
    private val proxyServer: ProxyServer
) {

    private val logger = Logger.getLogger(TabListHandler::class.java.name)
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
            logger.warning("Can't stop tablist task because it is not initialized")
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
