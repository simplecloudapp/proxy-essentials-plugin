package app.simplecloud.plugin.proxy.velocity.platform

import app.simplecloud.plugin.proxy.shared.ProxyPlatform
import com.velocitypowered.api.proxy.ProxyServer
import java.nio.file.Path

class VelocityPlatformImpl(
    private val server: ProxyServer,
    private val dataDirectory: Path
) : ProxyPlatform {

    override fun getDataDirectory() = dataDirectory

    override fun getOnlinePlayers() = server.playerCount

    override fun getMaxPlayers() = server.configuration.showMaxPlayers
}