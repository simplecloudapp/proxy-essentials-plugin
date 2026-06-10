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
                refresh()
                delay(proxyPlugin.proxyEssentialsConfig.get().playerCountUpdateTimeMillis())
            }
        }
    }

    fun stop() {
        syncJob?.cancel()
        scope.cancel()
    }

    fun onlinePlayersOr(fallback: Int): Int {
        return snapshot?.onlinePlayers ?: fallback
    }

    fun maxPlayersOr(fallback: Int): Int {
        return snapshot?.maxPlayers?.takeIf { it > 0 } ?: fallback
    }

    private suspend fun refresh() {
        try {
            snapshot = resolveSnapshot()
        } catch (e: Exception) {
            logger.severe("Error while syncing player count: ${e.message}")
        }
    }

    private suspend fun resolveSnapshot(): PlayerCountSnapshot? {
        val snapshots = resolveCountSnapshots()

        if (snapshots.isEmpty()) {
            return null
        }

        return PlayerCountSnapshot(
            onlinePlayers = snapshots.sumOf { it.onlinePlayers },
            maxPlayers = snapshots.sumOf { it.maxPlayers ?: 0 }.takeIf { it > 0 }
        )
    }

    private suspend fun resolveCountSnapshots(): List<PlayerCountSnapshot> {
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

        val groupNames = (
            listOfNotNull(currentGroupName) +
                config.additionalGroups
            )
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        val groupSnapshots = groupNames.mapNotNull { resolveGroupSnapshot(it) }

        val additionalPersistentServerNames = config.additionalPersistentServers
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filter { it != currentPersistentServerName }
            .toSet()
        val persistentServerSnapshots = proxyPlugin.cloudControllerHandler
            .getPersistentServersByNames(additionalPersistentServerNames)
            .map { resolveServerSnapshot(it) }

        return listOfNotNull(resolveCurrentServerSnapshot(currentServer)) + groupSnapshots + persistentServerSnapshots
    }

    private suspend fun resolveCurrentServerSnapshot(currentServer: Server?): PlayerCountSnapshot? {
        if (currentServer == null || currentServer.isFromGroup) {
            return null
        }

        val server = proxyPlugin.cloudControllerHandler.getServerById(currentServer.serverId) ?: currentServer
        return resolveServerSnapshot(server)
    }

    private suspend fun resolveGroupSnapshot(groupName: String): PlayerCountSnapshot? {
        val onlinePlayers = proxyPlugin.cloudControllerHandler.getOnlinePlayersInGroup(groupName)
        val maxPlayers = proxyPlugin.cloudControllerHandler.getMaxPlayersInGroup(groupName)
        if (onlinePlayers <= 0 && maxPlayers <= 0) {
            return null
        }

        return PlayerCountSnapshot(
            onlinePlayers = onlinePlayers,
            maxPlayers = maxPlayers.takeIf { it > 0 }
        )
    }

    private fun resolveServerSnapshot(server: Server): PlayerCountSnapshot {
        return PlayerCountSnapshot(
            onlinePlayers = server.playerCount?.toInt() ?: 0,
            maxPlayers = server.maxPlayers?.toInt()?.takeIf { it > 0 }
        )
    }

    private data class PlayerCountSnapshot(
        val onlinePlayers: Int,
        val maxPlayers: Int?
    )
}
