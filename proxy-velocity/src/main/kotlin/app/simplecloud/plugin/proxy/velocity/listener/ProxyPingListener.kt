package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.plugin.proxy.shared.config.motd.MaxPlayerDisplayType
import app.simplecloud.plugin.proxy.velocity.ProxyVelocityPlugin
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.proxy.server.ServerPing
import com.velocitypowered.api.proxy.server.ServerPing.SamplePlayer
import kotlinx.coroutines.runBlocking
import java.net.InetAddress
import java.util.*
import kotlin.jvm.optionals.getOrNull


class ProxyPingListener(
    private val plugin: ProxyVelocityPlugin
) {

    @Subscribe
    fun onProxyPing(event: ProxyPingEvent) {
        runBlocking {
            val serverPing = event.ping

            val motdConfiguration = plugin.motdLayoutHandler.getCurrentMotdLayout()

            val firstLine = motdConfiguration.firstLines.random()
            val secondLine = motdConfiguration.secondLines.random()

            val messageOfTheDay = plugin.deserializeToComponent("$firstLine\n$secondLine")

            val hostStringFromConnection = event.connection.remoteAddress.address.hostAddress
            val hostStringFromServer = InetAddress.getLocalHost().hostAddress

            var versions = serverPing.version
            var onlinePlayers = serverPing.players.getOrNull()?.online ?: 0
            var maxPlayers = serverPing.players.getOrNull()?.max ?: 0
            var samplePlayers = serverPing.players.getOrNull()?.sample ?: emptyList()

            if (hostStringFromConnection != hostStringFromServer) {
                val playerList = motdConfiguration.playerInfo.map { SamplePlayer(it, UUID.randomUUID()) }

                val players = serverPing.players.getOrNull()
                onlinePlayers = players?.online ?: 0
                val realMaxPlayers = players?.max ?: 0
                maxPlayers = when (motdConfiguration.maxPlayerDisplayType) {
                    MaxPlayerDisplayType.REAL -> realMaxPlayers
                    MaxPlayerDisplayType.DYNAMIC -> onlinePlayers + motdConfiguration.dynamicPlayerRange
                    null -> realMaxPlayers
                }

                samplePlayers = playerList.ifEmpty { players?.sample ?: emptyList() }

                versions = when (motdConfiguration.versionName) {
                    "" -> serverPing.version
                    else -> ServerPing.Version(
                        -1,
                        motdConfiguration.versionName
                    )
                }
            }


            val builder =  event.ping.asBuilder()
                .version(versions)
                .onlinePlayers(onlinePlayers)
                .maximumPlayers(maxPlayers)
                .samplePlayers(*samplePlayers.toTypedArray())
                .description(messageOfTheDay)

            event.ping = builder.build()
        }
    }

}