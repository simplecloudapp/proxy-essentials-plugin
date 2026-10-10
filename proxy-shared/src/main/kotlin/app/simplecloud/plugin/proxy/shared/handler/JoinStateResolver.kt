package app.simplecloud.plugin.proxy.shared.handler

import app.simplecloud.api.server.Server
import app.simplecloud.plugin.api.shared.pattern.ServerPatternIdentifier
import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import app.simplecloud.plugin.proxy.shared.config.state.JoinState
import java.util.logging.Logger

class JoinStateResolver(
    private val proxyPlugin: ProxyPlugin
) {
    private val logger = Logger.getLogger(JoinStateResolver::class.java.name)

    private val identifier by lazy {
        ServerPatternIdentifier(
            "<group_name>-<numerical_id>",
            "(?<groupName>[a-zA-Z0-9_-]+)-(?<numericalId>\\d+)",
            cloudApi = proxyPlugin.api
        )
    }

    fun resolveJoinState(stateName: String): JoinState? {
        val joinstates = proxyPlugin.proxyEssentialsConfig.get().joinstates
        val defaultStateName = proxyPlugin.proxyEssentialsConfig.get().initialState

        return joinstates.find { it.name == stateName }
            ?: run {
                logger.info("Join state '$stateName' not found. Using default '$defaultStateName'.")
                joinstates.find { it.name == defaultStateName }
            }
    }

    suspend fun getJoinStateForServer(serverName: String): String {
        val server = findCloudServer(serverName) ?: return getExternalJoinState(serverName)

        val joinState = server.properties?.get(JoinStateHandler.JOINSTATE_KEY)?.toString()
        if (!joinState.isNullOrBlank()) {
            return joinState
        }

        if (server.isFromGroup) {
            val groupName = server.group?.name
            if (groupName != null) {
                return proxyPlugin.joinStateHandler.getJoinStateAtGroup(groupName)
            }
        }

        val persistentName = server.persistentServer?.name
        if (persistentName != null) {
            return proxyPlugin.joinStateHandler.getJoinStateAtPersistentServer(persistentName)
        }

        return proxyPlugin.proxyEssentialsConfig.get().initialState
    }

    private fun getExternalJoinState(serverName: String): String {
        val config = proxyPlugin.proxyEssentialsConfig.get()
        val externalJoinState = config.externalServerJoinStates.entries
            .firstOrNull { it.key.equals(serverName, ignoreCase = true) }
            ?.value

        if (externalJoinState != null) {
            return externalJoinState
        }

        return config.initialState
    }

    private suspend fun findCloudServer(serverName: String): Server? {
        try {
            val (groupName, numericalId) = identifier.parse(serverName)
            val server = proxyPlugin.cloudControllerHandler.getServerByNumericalId(groupName, numericalId)
            if (server != null) {
                return server
            }
        } catch (_: IllegalArgumentException) {
        }

        return proxyPlugin.cloudControllerHandler.getServerByName(serverName)
    }

    suspend fun isServerFull(): Boolean {
        val server = proxyPlugin.cloudControllerHandler.currentServer ?: return false

        if (server.isFromGroup) {
            val groupName = server.group?.name ?: return false
            val maxPlayers = proxyPlugin.cloudControllerHandler.getMaxPlayersInGroup(groupName)
            if (maxPlayers <= 0) {
                return false
            }
            val onlinePlayers = proxyPlugin.cloudControllerHandler.getOnlinePlayersInGroup(groupName)
            return onlinePlayers >= maxPlayers
        }

        val maxPlayers = server.maxPlayers ?: return false
        val onlinePlayers = server.playerCount ?: 0
        return onlinePlayers >= maxPlayers
    }
}
