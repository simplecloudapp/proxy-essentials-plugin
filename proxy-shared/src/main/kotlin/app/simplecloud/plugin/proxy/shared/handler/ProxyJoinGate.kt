package app.simplecloud.plugin.proxy.shared.handler

import app.simplecloud.plugin.proxy.shared.config.message.KickMessageConfig
import app.simplecloud.plugin.proxy.shared.config.state.JoinState
import java.util.logging.Logger

class ProxyJoinGate(
    private val localState: () -> String,
    private val resolveJoinState: (String) -> JoinState?,
    private val isServerFull: suspend () -> Boolean,
    private val kickMessages: () -> KickMessageConfig
) {
    private val logger = Logger.getLogger(ProxyJoinGate::class.java.name)

    sealed interface Result {
        data object Allowed : Result
        data class Denied(val kickMessage: String) : Result
    }

    suspend fun evaluate(playerName: String, hasPermission: (String) -> Boolean): Result {
        val stateName = localState()
        val joinState = resolveJoinState(stateName)
        val kick = kickMessages()

        if (joinState == null) {
            logger.severe("Neither join state '$stateName' nor default state found. Check configuration!")
            return Result.Denied(kick.noJoinState)
        }

        if (joinState.permission.join.isNotBlank() && !hasPermission(joinState.permission.join)) {
            logger.info("Player $playerName does not have permission to join the proxy. (JoinState: $stateName, Permission: ${joinState.permission.join})")
            return Result.Denied(kick.noPermission)
        }

        try {
            if (isServerFull() && !hasPermission(joinState.permission.full)) {
                return Result.Denied(kick.networkFull)
            }
        } catch (e: Exception) {
            logger.severe("Error checking player limits: ${e.message}")
        }

        return Result.Allowed
    }
}
