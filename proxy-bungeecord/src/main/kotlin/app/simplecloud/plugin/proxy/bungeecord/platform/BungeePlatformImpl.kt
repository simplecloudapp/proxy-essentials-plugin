package app.simplecloud.plugin.proxy.bungeecord.platform

import app.simplecloud.plugin.proxy.shared.platform.ProxyPlatform
import net.md_5.bungee.api.ProxyServer
import java.nio.file.Path

class BungeePlatformImpl(
    private val proxy: ProxyServer,
    private val dataDirectory: Path
) : ProxyPlatform {

    override fun getDataDirectory() = dataDirectory

    override fun getOnlinePlayers() = proxy.onlineCount

    override fun getMaxPlayers() = proxy.config.playerLimit

    override fun getPlayers() = proxy.players.map { BungeeProxyPlayer(it) }
}