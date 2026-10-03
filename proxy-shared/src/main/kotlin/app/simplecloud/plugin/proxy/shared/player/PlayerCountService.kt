package app.simplecloud.plugin.proxy.shared.player

import app.simplecloud.api.CloudApi
import app.simplecloud.api.runtime.SimpleCloudRuntime
import app.simplecloud.plugin.api.shared.config.ConfigurationFactory
import app.simplecloud.plugin.proxy.shared.platform.ProxyPlatform
import app.simplecloud.plugin.proxy.shared.config.PlayerCountConfig
import app.simplecloud.plugin.proxy.shared.config.ProxyEssentialsConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class PlayerCountService(
    private val api: CloudApi,
    private val platform: ProxyPlatform,
    private val config: ConfigurationFactory<ProxyEssentialsConfig>,
    private val scope: CoroutineScope
) {

    private val logger = LoggerFactory.getLogger(PlayerCountService::class.java)
    private val otherOnlinePlayers = AtomicInteger()
    private val networkMaxPlayers = AtomicInteger()

    fun getOnlinePlayers(): Int = platform.getOnlinePlayers() + otherOnlinePlayers.get()

    fun getMaxPlayers(): Int {
        val maxPlayers = networkMaxPlayers.get()
        if (maxPlayers <= 0) return platform.getMaxPlayers()
        return maxPlayers
    }

    fun start() {
        scope.launch {
            while (isActive) {
                val playerCount = config.get().playerCount
                update(playerCount)
                if (playerCount.updateTime <= 0) return@launch

                delay((playerCount.updateTime * 50L).milliseconds)
            }
        }
    }

    private suspend fun update(config: PlayerCountConfig) {
        if (!config.enabled) {
            otherOnlinePlayers.set(0)
            networkMaxPlayers.set(0)
            return
        }

        try {
            updateCounts(config)
        } catch (e: Exception) {
            logger.error("Could not count the players of the network, showing the last known count", e)
        }
    }

    private suspend fun updateCounts(config: PlayerCountConfig) {
        val server = api.server().getServerById(SimpleCloudRuntime.serverId()).await()
        val groupNames = (listOfNotNull(server.group?.name) + config.additionalGroups).distinct()
        val groups = groupNames.map { api.group().getGroupByName(it).await() }
        val groupServers = groupNames.flatMap { api.server().getServersByGroup(it).await() }
        val persistentServers = api.server().allServers.await().filter { it.persistentServer?.name in config.additionalPersistentServers }

        val ownMaxPlayers = if (server.group == null) server.maxPlayers ?: 0 else 0
        val otherServers = (groupServers + persistentServers).filter { it.serverId != server.serverId }

        otherOnlinePlayers.set(otherServers.sumOf { it.playerCount ?: 0 })
        networkMaxPlayers.set(ownMaxPlayers + groups.sumOf { it.maxPlayers ?: 0 } + persistentServers.sumOf { it.maxPlayers ?: 0 })
    }

}
