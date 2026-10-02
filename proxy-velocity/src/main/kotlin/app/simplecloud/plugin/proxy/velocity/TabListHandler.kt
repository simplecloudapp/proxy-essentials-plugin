package app.simplecloud.plugin.proxy.velocity

import app.simplecloud.plugin.proxy.shared.ProxyEssentials
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import java.util.concurrent.TimeUnit
import kotlin.jvm.optionals.getOrNull

class TabListHandler(
    private val plugin: VelocityProxyPlugin,
    private val server: ProxyServer,
    private val essentials: ProxyEssentials
) {

    fun start() {
        val interval = essentials.tabListService.getUpdateIntervalMillis()

        server.scheduler
            .buildTask(plugin, Runnable { update(interval != null) })
            .delay(interval ?: 1000L, TimeUnit.MILLISECONDS)
            .schedule()
    }

    fun update(player: Player) {
        val serverName = player.currentServer.getOrNull()?.serverInfo?.name ?: return
        val tabList = essentials.tabListService.getTabList(serverName) ?: return
        val formatter = essentials.messageFormatter

        player.sendPlayerListHeaderAndFooter(
            formatter.formatForPlayer(tabList.header, serverName, player.ping),
            formatter.formatForPlayer(tabList.footer, serverName, player.ping)
        )
    }

    private fun update(enabled: Boolean) {
        try {
            if (enabled) server.allPlayers.forEach { update(it) }
        } finally {
            start()
        }
    }
}
