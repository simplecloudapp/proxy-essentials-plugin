package app.simplecloud.plugin.proxy.bungeecord.handler

import app.simplecloud.plugin.proxy.bungeecord.ProxyBungeeCordPlugin
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.scheduler.ScheduledTask
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

class TabListHandler(
    private val plugin: ProxyBungeeCordPlugin
) {
    private val logger = Logger.getLogger(TabListHandler::class.java.name)
    private val resolver = plugin.proxyPlugin.tabListResolver
    private lateinit var task: ScheduledTask

    fun startTabListTask() {
        task = plugin.proxy.scheduler.schedule(plugin, {
            resolver.incrementIndices()
            plugin.proxy.players.forEach { updateTabListForPlayer(it) }
        }, 1, plugin.proxyPlugin.tabListConfiguration.get().tabListUpdateTime, TimeUnit.MILLISECONDS)
    }

    fun stopTabListTask() {
        if (!this::task.isInitialized) {
            logger.warning("Can't stop tablist task because it is not initialized")
            return
        }
        task.cancel()
    }

    fun updateTabListForPlayer(player: ProxiedPlayer) {
        if (player.server == null || player.server.info == null) return

        val serviceName = player.server.info.name
        val tabListGroup = resolver.findTabListGroup(serviceName) ?: return
        val (header, footer) = resolver.getCurrentTabList(tabListGroup) ?: return

        plugin.adventure().player(player).sendPlayerListHeaderAndFooter(
            plugin.deserializeToComponent(header, player),
            plugin.deserializeToComponent(footer, player),
        )
    }
}