package app.simplecloud.plugin.proxy.shared.joinstate

import app.simplecloud.api.CloudApi
import app.simplecloud.api.runtime.SimpleCloudRuntime
import app.simplecloud.plugin.api.shared.config.ConfigurationFactory
import app.simplecloud.plugin.proxy.shared.config.JoinStateConfig
import app.simplecloud.plugin.proxy.shared.config.MessageConfig
import app.simplecloud.plugin.proxy.shared.config.ProxyEssentialsConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

class JoinGate(
    private val api: CloudApi,
    private val service: JoinStateService,
    private val config: ConfigurationFactory<ProxyEssentialsConfig>,
    private val messages: ConfigurationFactory<MessageConfig>,
    private val scope: CoroutineScope
) {

    private val logger = LoggerFactory.getLogger(JoinGate::class.java)
    private val proxyFull = AtomicBoolean(false)

    fun start() {
        scope.launch {
            while (isActive) {
                try {
                    proxyFull.set(isProxyFull())
                } catch (e: Exception) {
                    logger.error("Could not check whether the proxy is full, keeping the last known value", e)
                }
                delay(2.seconds)
            }
        }
    }

    fun checkProxyJoin(name: String, uuid: UUID, hasPermission: (String) -> Boolean): JoinResult {
        if (config.get().whitelist.contains(name, uuid)) return JoinResult.Allowed

        val kick = messages.get().kick
        val joinState = findJoinState(service.getLocalState()) ?: return JoinResult.Denied(kick.noJoinState)
        if (!hasJoinPermission(joinState, hasPermission)) {
            logger.info("$name is missing the permission '${joinState.permission.join}' to join the proxy")
            return JoinResult.Denied(kick.noPermission)
        }

        if (proxyFull.get() && !hasPermission(joinState.permission.full)) return JoinResult.Denied(kick.networkFull)
        return JoinResult.Allowed
    }

    fun checkServerSwitch(name: String, uuid: UUID, serverName: String, hasPermission: (String) -> Boolean): JoinResult {
        if (config.get().whitelist.contains(name, uuid)) return JoinResult.Allowed

        val kick = messages.get().kick
        val state = service.getServerState(serverName)
        if (state == null) {
            logger.warn("The join states of the servers are not loaded yet, so $name cannot join '$serverName'")
            return JoinResult.Denied(kick.noJoinState)
        }

        val joinState = findJoinState(state) ?: return JoinResult.Denied(kick.noJoinState)
        if (!hasJoinPermission(joinState, hasPermission)) {
            logger.info("$name is missing the permission '${joinState.permission.join}' to join '$serverName'")
            return JoinResult.Denied(kick.noPermission)
        }

        return JoinResult.Allowed
    }

    private fun findJoinState(name: String): JoinStateConfig? {
        val joinState = config.get().joinstates.find { it.name == name }
        if (joinState == null) {
            logger.error("The join state '$name' does not exist in config.yml, so nobody can join until it is added or the join state is changed.")
        }
        return joinState
    }

    private fun hasJoinPermission(joinState: JoinStateConfig, hasPermission: (String) -> Boolean): Boolean {
        val permission = joinState.permission.join
        return permission.isBlank() || hasPermission(permission)
    }

    private suspend fun isProxyFull(): Boolean {
        val server = api.server().getServerById(SimpleCloudRuntime.serverId()).await()
        val group = server.group
        if (group == null) {
            val maxPlayers = server.maxPlayers ?: 0
            return maxPlayers in 1..(server.playerCount ?: 0)
        }

        val maxPlayers = group.maxPlayers ?: 0
        val onlinePlayers = api.server().getServersByGroup(group.name).await().sumOf { it.playerCount ?: 0 }
        return maxPlayers in 1..onlinePlayers
    }
}
