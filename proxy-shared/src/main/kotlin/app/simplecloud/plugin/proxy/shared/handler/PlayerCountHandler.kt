package app.simplecloud.plugin.proxy.shared.handler

import app.simplecloud.api.server.Server
import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import kotlinx.coroutines.*
import java.util.logging.Logger

class PlayerCountHandler(
    private val proxyPlugin: ProxyPlugin
) {
    private val logger = Logger.getLogger(PlayerCountHandler::class.java.name)
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
                val updateTimeMillis = proxyPlugin.proxyEssentialsConfig.get().playerCountUpdateTimeMillis()
                if (updateTimeMillis == null) {
                    snapshot = null
                    delay(DISABLED_CHECK_INTERVAL_MILLIS)
                    continue
                }

                refresh()
                delay(updateTimeMillis)
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
        return snapshot?.maxPlayers?.takeIf { it > 0 } ?: currentProxyMaxPlayers
    }

    private suspend fun refresh() {
        try {
            snapshot = resolveSnapshot()
        } catch (e: Exception) {
            logger.severe("Error while syncing player count: ${e.message}")
        }
    }

    private suspend fun resolveSnapshot(): PlayerCountSnapshot? {
        val count = resolveCount()

        if (count.otherOnlinePlayers <= 0 && count.maxPlayers <= 0) {
            return null
        }

        return PlayerCountSnapshot(
            otherOnlinePlayers = count.otherOnlinePlayers,
            maxPlayers = count.maxPlayers.takeIf { it > 0 }
        )
    }

    private suspend fun resolveCount(): PlayerCount {
        val currentServer = proxyPlugin.cloudControllerHandler.currentServer
        val config = proxyPlugin.proxyEssentialsConfig.get().playerCount
        val currentGroupName = currentServer
            ?.takeIf { it.isFromGroup }
            ?.group
            ?.name
        val currentPersistentServerName = currentServer
            ?.takeIf { it.isFromPersistentServer }
            ?.persistentServer
            ?.name

        val currentServerId = currentServer?.serverId
        val additionalGroupNames = config.additionalGroups
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filter { it != currentGroupName }
            .distinct()

        val additionalPersistentServerNames = config.additionalPersistentServers
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filter { it != currentPersistentServerName }
            .toSet()

        val currentGroupOtherOnlinePlayers = currentGroupName
            ?.let { resolveGroupOnlinePlayers(it, currentServerId) }
            ?: 0
        val currentMaxPlayers = resolveCurrentMaxPlayers(currentServer, currentGroupName)
        val groupOnlinePlayers = additionalGroupNames.sumOf { proxyPlugin.cloudControllerHandler.getOnlinePlayersInGroup(it) }
        val groupMaxPlayers = additionalGroupNames.sumOf { proxyPlugin.cloudControllerHandler.getMaxPlayersInGroup(it) }
        val persistentServers = proxyPlugin.cloudControllerHandler
            .getPersistentServersByNames(additionalPersistentServerNames)
        val persistentOnlinePlayers = persistentServers.sumOf { it.playerCount?.toInt() ?: 0 }
        val persistentMaxPlayers = persistentServers.sumOf { it.maxPlayers?.toInt() ?: 0 }

        return PlayerCount(
            otherOnlinePlayers = currentGroupOtherOnlinePlayers + groupOnlinePlayers + persistentOnlinePlayers,
            maxPlayers = currentMaxPlayers + groupMaxPlayers + persistentMaxPlayers
        )
    }

    private suspend fun resolveGroupOnlinePlayers(groupName: String, excludedServerId: String?): Int {
        return proxyPlugin.cloudControllerHandler
            .getServersByGroup(groupName)
            .asSequence()
            .filter { it.serverId != excludedServerId }
            .sumOf { it.playerCount?.toInt() ?: 0 }
    }

    private suspend fun resolveCurrentMaxPlayers(currentServer: Server?, currentGroupName: String?): Int {
        if (currentServer == null) {
            return 0
        }

        if (currentGroupName != null) {
            return proxyPlugin.cloudControllerHandler.getMaxPlayersInGroup(currentGroupName)
        }

        return currentServer.maxPlayers?.toInt() ?: 0
    }

    private data class PlayerCount(
        val otherOnlinePlayers: Int,
        val maxPlayers: Int
    )

    private data class PlayerCountSnapshot(
        val otherOnlinePlayers: Int,
        val maxPlayers: Int?
    )

    private companion object {
        const val DISABLED_CHECK_INTERVAL_MILLIS = 1000L
    }
}
