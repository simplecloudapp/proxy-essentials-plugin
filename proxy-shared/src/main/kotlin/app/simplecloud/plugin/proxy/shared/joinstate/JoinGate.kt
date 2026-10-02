package app.simplecloud.plugin.proxy.shared.joinstate

import app.simplecloud.api.CloudApi
import app.simplecloud.api.runtime.SimpleCloudRuntime
import app.simplecloud.plugin.api.shared.config.ConfigurationFactory
import app.simplecloud.plugin.proxy.shared.config.JoinStateConfig
import app.simplecloud.plugin.proxy.shared.config.MessageConfig
import app.simplecloud.plugin.proxy.shared.config.ProxyEssentialsConfig
import kotlinx.coroutines.future.await
import org.slf4j.LoggerFactory
import java.util.UUID

class JoinGate(
    private val api: CloudApi,
    private val service: JoinStateService,
    private val config: ConfigurationFactory<ProxyEssentialsConfig>,
    private val messages: ConfigurationFactory<MessageConfig>
) {

    private val logger = LoggerFactory.getLogger(JoinGate::class.java)

    suspend fun checkProxyJoin(name: String, uuid: UUID, hasPermission: (String) -> Boolean): JoinResult {
        if (config.get().whitelist.contains(name, uuid)) return JoinResult.Allowed

        val kick = messages.get().kick
        val joinState = findJoinState(service.getLocalState()) ?: return JoinResult.Denied(kick.noJoinState)
        if (!hasJoinPermission(joinState, hasPermission)) {
            logger.info("$name is missing the permission '${joinState.permission.join}' to join the proxy")
            return JoinResult.Denied(kick.noPermission)
        }

        if (isProxyFull() && !hasPermission(joinState.permission.full)) return JoinResult.Denied(kick.networkFull)
        return JoinResult.Allowed
    }

    suspend fun checkServerSwitch(name: String, uuid: UUID, serverName: String, hasPermission: (String) -> Boolean): JoinResult {
        if (config.get().whitelist.contains(name, uuid)) return JoinResult.Allowed

        val kick = messages.get().kick
        val joinState = findJoinState(getServerState(serverName)) ?: return JoinResult.Denied(kick.noJoinState)
        if (!hasJoinPermission(joinState, hasPermission)) {
            logger.info("$name is missing the permission '${joinState.permission.join}' to join '$serverName'")
            return JoinResult.Denied(kick.noPermission)
        }

        return JoinResult.Allowed
    }

    private fun findJoinState(name: String): JoinStateConfig? {
        val config = config.get()
        val joinState = config.joinstates.find { it.name == name } ?: config.joinstates.find { it.name == config.initialState }
        if (joinState == null) {
            logger.error("Neither the join state '$name' nor the initial state '${config.initialState}' exist in config.yml, so nobody can join.")
        }
        return joinState
    }

    private fun hasJoinPermission(joinState: JoinStateConfig, hasPermission: (String) -> Boolean): Boolean {
        val permission = joinState.permission.join
        return permission.isBlank() || hasPermission(permission)
    }

    private suspend fun getServerState(serverName: String): String {
        return try {
            service.getServerState(serverName)
        } catch (e: Exception) {
            logger.error("Could not load the join state of '$serverName', using the initial state instead", e)
            config.get().initialState
        }
    }

    private suspend fun isProxyFull(): Boolean {
        val serverId = SimpleCloudRuntime.serverId()
        if (serverId.isBlank()) return false

        return try {
            val server = api.server().getServerById(serverId).await()
            val group = server.group
            if (group == null) {
                (server.playerCount ?: 0) >= server.maxPlayers
            } else {
                val maxPlayers = group.maxPlayers ?: 0
                val onlinePlayers = api.server().getServersByGroup(group.name).await().sumOf { it.playerCount ?: 0 }
                maxPlayers in 1..onlinePlayers
            }
        } catch (e: Exception) {
            logger.error("Could not check whether the proxy is full, letting the player join", e)
            false
        }
    }
}
