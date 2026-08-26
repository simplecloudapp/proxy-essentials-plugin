package app.simplecloud.plugin.proxy.shared.joinstate

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import java.util.logging.Logger

class ProxyJoinGate(
    private val plugin: ProxyPlugin
) {

    private val logger = Logger.getLogger(ProxyJoinGate::class.java.name)

    sealed interface Result {
        data object Allowed : Result
        data class Denied(val kickMessage: String) : Result
    }

    suspend fun evaluate(playerName: String, hasPermission: (String) -> Boolean): Result {
        val stateName = plugin.joinStateService.localState
        val kickMessages = plugin.messageConfig.get().kick
        val joinState = plugin.joinStateResolver.resolveJoinState(stateName)

        if (joinState == null) {
            logger.severe("Neither join state '$stateName' nor default state found. Check configuration!")
            return Result.Denied(kickMessages.noJoinState)
        }

        val joinPermission = joinState.permission.join
        if (joinPermission.isNotBlank() && !hasPermission(joinPermission)) {
            logger.info("Player $playerName does not have permission to join the proxy. (JoinState: $stateName, Permission: $joinPermission)")
            return Result.Denied(kickMessages.noPermission)
        }

        if (isNetworkFull(hasPermission, joinState.permission.full)) {
            return Result.Denied(kickMessages.networkFull)
        }

        return Result.Allowed
    }

    private suspend fun isNetworkFull(hasPermission: (String) -> Boolean, fullPermission: String): Boolean {
        return try {
            plugin.joinStateResolver.isServerFull() && !hasPermission(fullPermission)
        } catch (e: Exception) {
            logger.severe("Error checking player limits: ${e.message}")
            false
        }
    }
}
