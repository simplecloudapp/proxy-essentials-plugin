package app.simplecloud.plugin.proxy.shared

import java.nio.file.Path

interface ProxyPlatform {

    fun getDataDirectory(): Path

    fun getOnlinePlayers(): Int

    fun getMaxPlayers(): Int
}
