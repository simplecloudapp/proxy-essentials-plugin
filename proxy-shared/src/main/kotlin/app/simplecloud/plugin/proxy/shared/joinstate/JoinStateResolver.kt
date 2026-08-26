package app.simplecloud.plugin.proxy.shared.joinstate

import app.simplecloud.plugin.api.shared.pattern.ServerPatternIdentifier
import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import app.simplecloud.plugin.proxy.shared.config.JoinState
import java.util.logging.Logger

class JoinStateResolver(
    private val plugin: ProxyPlugin
) {

    private val logger = Logger.getLogger(JoinStateResolver::class.java.name)

    private val identifier by lazy {
        ServerPatternIdentifier(
            "<group_name>-<numerical_id>",
            "(?<groupName>[a-zA-Z0-9_-]+)-(?<numericalId>\\d+)",
            cloudApi = plugin.api
        )
    }

    fun resolveJoinState(stateName: String): JoinState? {
        val config = plugin.config.get()
        val joinState = config.joinstates.find { it.name == stateName }
        if (joinState != null) {
            return joinState
        }

        logger.info("Join state '$stateName' not found. Using default '${config.initialState}'.")
        return config.joinstates.find { it.name == config.initialState }
    }

    suspend fun getJoinStateForServer(serverName: String): String {
        val serviceJoinState = getJoinStateOfService(serverName)
        if (serviceJoinState != null) {
            return serviceJoinState
        }

        val server = plugin.cloudControllerHandler.getServerByName(serverName)
            ?: return plugin.config.get().initialState

        val joinState = server.properties?.get(JoinStateService.KEY)?.toString()
        if (!joinState.isNullOrEmpty()) {
            return joinState
        }

        val persistentServerName = server.persistentServer?.name
            ?: return plugin.config.get().initialState

        return plugin.joinStateService.getJoinStateAtPersistentServer(persistentServerName)
    }

    private suspend fun getJoinStateOfService(serverName: String): String? {
        val (groupName, numericalId) = try {
            identifier.parse(serverName)
        } catch (_: IllegalArgumentException) {
            return null
        }

        return plugin.joinStateService.getJoinStateAtService(groupName, numericalId)
    }

    suspend fun isServerFull(): Boolean {
        val server = plugin.cloudControllerHandler.currentServer ?: return false

        if (!server.isFromGroup) {
            val maxPlayers = server.maxPlayers ?: return false
            return (server.playerCount ?: 0) >= maxPlayers
        }

        val groupName = server.group?.name ?: return false
        val maxPlayers = plugin.cloudControllerHandler.getMaxPlayersInGroup(groupName)
        if (maxPlayers <= 0) {
            return false
        }

        return plugin.cloudControllerHandler.getOnlinePlayersInGroup(groupName) >= maxPlayers
    }
}
