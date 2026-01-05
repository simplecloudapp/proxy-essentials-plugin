package app.simplecloud.plugin.proxy.shared.handler

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
            proxyPlugin.joinStateConfiguration.get().serverNamePattern,
            cloudApi = proxyPlugin.api
        )
    }

    fun resolveJoinState(stateName: String): JoinState? {
        val joinStates = proxyPlugin.joinStateConfiguration.get().joinStates
        val defaultStateName = proxyPlugin.joinStateConfiguration.get().defaultState

        return joinStates.find { it.name == stateName }
            ?: run {
                logger.info("Join state '$stateName' not found. Using default '$defaultStateName'.")
                joinStates.find { it.name == defaultStateName }
            }
    }

    suspend fun getJoinStateForServer(serverName: String): String {
        try {
            val (groupName, numericalId) = identifier.parse(serverName)
            return proxyPlugin.joinStateHandler.getJoinStateAtService(groupName, numericalId)
        } catch (_: IllegalArgumentException) {
        }

        val server = proxyPlugin.cloudControllerHandler.getServerByName(serverName)
        if (server != null) {
            val joinState = server.properties?.get(JoinStateHandler.JOINSTATE_KEY)?.toString()
            if (!joinState.isNullOrEmpty()) {
                return joinState
            }
        }

        return proxyPlugin.joinStateConfiguration.get().defaultState
    }

    suspend fun isServerFull(): Boolean {
        val server = proxyPlugin.cloudControllerHandler.currentServer ?: return false

        if (server.isFromGroup) {
            val groupName = server.group?.name ?: return false
            val maxPlayers = proxyPlugin.cloudControllerHandler.getMaxPlayersInGroup(groupName)
            val onlinePlayers = proxyPlugin.cloudControllerHandler.getOnlinePlayersInGroup(groupName)
            return onlinePlayers >= maxPlayers
        }

        val maxPlayers = server.maxPlayers ?: return false
        val onlinePlayers = server.playerCount ?: 0
        return onlinePlayers >= maxPlayers
    }
}
