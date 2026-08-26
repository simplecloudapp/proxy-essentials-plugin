package app.simplecloud.plugin.proxy.bungeecord.tablist

import app.simplecloud.plugin.proxy.bungeecord.ProxyBungeeCordPlugin
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.scheduler.ScheduledTask
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

class TabListHandler(
    private val plugin: ProxyBungeeCordPlugin
) {

    private val logger = LoggerFactory.getLogger(TabListHandler::class.java)
    private var task: ScheduledTask? = null

    fun startTabListTask() {
        val updateTimeMillis = plugin.proxyPlugin.config.get().tabListUpdateTimeMillis()

        task = plugin.proxy.scheduler.schedule(
            plugin,
            { updateTabLists() },
            1,
            updateTimeMillis,
            TimeUnit.MILLISECONDS
        )
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

    fun updateTabListForPlayer(player: ProxiedPlayer) {
        val serverInfo = player.server?.info ?: return
        val resolver = plugin.proxyPlugin.tabListResolver
        val tabListGroup = resolver.findTabListGroup(serverInfo.name) ?: return
        val (header, footer) = resolver.getCurrentTabList(tabListGroup) ?: return

        plugin.adventure().player(player).sendPlayerListHeaderAndFooter(
            plugin.deserializeToComponent(header, player),
            plugin.deserializeToComponent(footer, player),
        )
    }

    private fun updateTabLists() {
        plugin.proxyPlugin.tabListResolver.incrementIndices()
        plugin.proxy.players.forEach { updateTabListForPlayer(it) }
    }
}
