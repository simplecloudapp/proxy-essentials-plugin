package app.simplecloud.plugin.proxy.shared.player

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import app.simplecloud.plugin.proxy.shared.config.PlayerCountConfig
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.milliseconds

class PlayerCountTracker(
    private val plugin: ProxyPlugin
) {

    private val logger = LoggerFactory.getLogger(PlayerCountTracker::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var syncJob: Job? = null

    @Volatile
    private var snapshot: PlayerCountSnapshot? = null

    fun start() {
        if (syncJob?.isActive == true) {
            return
        }

        syncJob = scope.launch {
            while (isActive) {
                val updateTime = plugin.config.get().playerCountUpdateTimeMillis()
                if (updateTime == null) {
                    snapshot = null
                    delay(1000.milliseconds)
                    continue
                }

                refresh()
                delay(updateTime.milliseconds)
            }
        }
    }

    fun stop() {
        syncJob?.cancel()
        scope.cancel()
    }

    fun onlinePlayers(currentProxyOnlinePlayers: Int): Int {
        return currentProxyOnlinePlayers + (snapshot?.otherOnlinePlayers ?: 0)
    }

    fun maxPlayers(currentProxyMaxPlayers: Int): Int {
        val maxPlayers = snapshot?.maxPlayers ?: return currentProxyMaxPlayers
        if (maxPlayers <= 0) return currentProxyMaxPlayers
        return maxPlayers
    }

    private suspend fun refresh() {
        try {
            snapshot = resolveSnapshot()
        } catch (e: Exception) {
            logger.error("Error while syncing player count", e)
        }
    }

    private suspend fun resolveSnapshot(): PlayerCountSnapshot? {
        val config = plugin.config.get().playerCount
        val currentGroupName = getCurrentGroupName()

        val ownGroup = resolveOwnGroupCount(currentGroupName)
        val additionalGroups = resolveAdditionalGroupCount(config, currentGroupName)
        val persistentServers = resolvePersistentServerCount(config)

        val otherOnlinePlayers = ownGroup.otherOnlinePlayers +
            additionalGroups.otherOnlinePlayers +
            persistentServers.otherOnlinePlayers
        val maxPlayers = ownGroup.maxPlayers + additionalGroups.maxPlayers + persistentServers.maxPlayers

        if (otherOnlinePlayers <= 0 && maxPlayers <= 0) {
            return null
        }

        return PlayerCountSnapshot(
            otherOnlinePlayers = otherOnlinePlayers,
            maxPlayers = maxPlayers
        )
    }

    private suspend fun resolveOwnGroupCount(currentGroupName: String?): PlayerCount {
        val currentServer = plugin.cloudControllerHandler.currentServer ?: return PlayerCount(0, 0)

        if (currentGroupName == null) {
            return PlayerCount(otherOnlinePlayers = 0, maxPlayers = currentServer.maxPlayers ?: 0)
        }

        val otherOnlinePlayers = plugin.cloudControllerHandler.getServersByGroup(currentGroupName)
            .filter { it.serverId != currentServer.serverId }
            .sumOf { it.playerCount ?: 0 }

        return PlayerCount(
            otherOnlinePlayers = otherOnlinePlayers,
            maxPlayers = plugin.cloudControllerHandler.getMaxPlayersInGroup(currentGroupName)
        )
    }

    private suspend fun resolveAdditionalGroupCount(
        config: PlayerCountConfig,
        currentGroupName: String?
    ): PlayerCount {
        val groupNames = config.additionalGroups
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != currentGroupName }
            .distinct()

        return PlayerCount(
            otherOnlinePlayers = groupNames.sumOf { plugin.cloudControllerHandler.getOnlinePlayersInGroup(it) },
            maxPlayers = groupNames.sumOf { plugin.cloudControllerHandler.getMaxPlayersInGroup(it) }
        )
    }

    private suspend fun resolvePersistentServerCount(config: PlayerCountConfig): PlayerCount {
        val serverNames = config.additionalPersistentServers
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != getCurrentPersistentServerName() }
            .toSet()

        val servers = plugin.cloudControllerHandler.getPersistentServersByNames(serverNames)

        return PlayerCount(
            otherOnlinePlayers = servers.sumOf { it.playerCount ?: 0 },
            maxPlayers = servers.sumOf { it.maxPlayers ?: 0 }
        )
    }

    private fun getCurrentGroupName(): String? {
        val currentServer = plugin.cloudControllerHandler.currentServer ?: return null
        if (!currentServer.isFromGroup) return null
        return currentServer.group?.name
    }

    private fun getCurrentPersistentServerName(): String? {
        val currentServer = plugin.cloudControllerHandler.currentServer ?: return null
        if (!currentServer.isFromPersistentServer) return null
        return currentServer.persistentServer?.name
    }

    private data class PlayerCount(
        val otherOnlinePlayers: Int,
        val maxPlayers: Int
    )

    private data class PlayerCountSnapshot(
        val otherOnlinePlayers: Int,
        val maxPlayers: Int
    )
}
