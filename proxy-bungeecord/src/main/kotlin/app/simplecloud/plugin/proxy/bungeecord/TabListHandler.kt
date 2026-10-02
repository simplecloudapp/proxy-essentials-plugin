package app.simplecloud.plugin.proxy.bungeecord

import app.simplecloud.plugin.proxy.shared.ProxyEssentials
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer
import net.md_5.bungee.api.connection.ProxiedPlayer
import java.util.concurrent.TimeUnit

class TabListHandler(
    private val plugin: ProxyBungeeCordPlugin,
    private val essentials: ProxyEssentials
) {

    fun start() {
        val interval = essentials.tabListService.getUpdateIntervalMillis()
        plugin.proxy.scheduler.schedule(plugin, { update(interval != null) }, interval ?: 1000L, TimeUnit.MILLISECONDS)
    }

    fun update(player: ProxiedPlayer) {
        val serverName = player.server?.info?.name ?: return
        val tabList = essentials.tabListService.getTabList(serverName) ?: return
        val formatter = essentials.messageFormatter
        val serializer = BungeeComponentSerializer.get()

        player.setTabHeader(
            serializer.serialize(formatter.formatForPlayer(tabList.header, serverName, player.ping.toLong())),
            serializer.serialize(formatter.formatForPlayer(tabList.footer, serverName, player.ping.toLong()))
        )
    }

    private fun update(enabled: Boolean) {
        try {
            if (enabled) plugin.proxy.players.forEach { update(it) }
        } finally {
            start()
        }
    }
}
