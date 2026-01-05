package app.simplecloud.plugin.proxy.velocity.handler

import app.simplecloud.plugin.proxy.velocity.ProxyVelocityPlugin
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.scheduler.ScheduledTask
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

class TabListHandler(
    private val plugin: ProxyVelocityPlugin
) {
    private val logger = Logger.getLogger(TabListHandler::class.java.name)
    private val resolver = plugin.tabListResolver
    private lateinit var task: ScheduledTask

    fun startTabListTask() {
        task = plugin.proxyServer.scheduler.buildTask(plugin, Runnable {
            resolver.incrementIndices()
            plugin.proxyServer.allPlayers.forEach { updateTabListForPlayer(it) }
        }).repeat(plugin.tabListConfiguration.get().tabListUpdateTime, TimeUnit.MILLISECONDS).schedule()
    }

    fun stopTabListTask() {
        if (!this::task.isInitialized) {
            logger.warning("Can't stop tablist task because it is not initialized")
            return
        }
        task.cancel()
    }

    fun updateTabListForPlayer(player: Player) {
        if (player.currentServer.isEmpty) return

        val serviceName = player.currentServer.get().serverInfo.name
        val tabListGroup = resolver.findTabListGroup(serviceName) ?: return
        val (header, footer) = resolver.getCurrentTabList(tabListGroup) ?: return

        player.sendPlayerListHeaderAndFooter(
            plugin.deserializeToComponent(header, player),
            plugin.deserializeToComponent(footer, player),
        )
    }
}