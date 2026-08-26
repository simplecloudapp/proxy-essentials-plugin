package app.simplecloud.plugin.proxy.shared.joinstate

import app.simplecloud.api.runtime.SimpleCloudRuntime
import app.simplecloud.api.server.Server
import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.logging.Logger
import kotlin.time.Duration.Companion.milliseconds

class JoinStateService(
    private val plugin: ProxyPlugin
) {

    private val logger = Logger.getLogger(JoinStateService::class.java.name)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var syncJob: Job? = null

    @Volatile
    private var lastObservedGroupState: String? = null

    @Volatile
    var localState: String = plugin.config.get().initialState

    fun stop() {
        syncJob?.cancel()
        scope.cancel()
    }

    suspend fun getJoinStateAtGroup(groupName: String): String {
        return controller().getGroupProperty(groupName, KEY) ?: defaultJoinState()
    }

    suspend fun setJoinStateAtGroup(groupName: String, joinStateName: String): Boolean {
        val updated = controller().updateGroupProperty(groupName, KEY, joinStateName)
        if (updated) {
            lastObservedGroupState = joinStateName
        }
        return updated
    }

    suspend fun ensureJoinStateAtGroup(groupName: String): String {
        val existingJoinState = controller().getGroupProperty(groupName, KEY)
        if (!existingJoinState.isNullOrBlank()) {
            return existingJoinState
        }

        val fallbackState = defaultJoinState()
        if (!setJoinStateAtGroup(groupName, fallbackState)) {
            logger.warning("Could not persist default join state '$fallbackState' for group '$groupName'")
        }
        return fallbackState
    }

    suspend fun getJoinStateAtService(groupName: String, numericalId: Int): String {
        return controller().getServiceProperty(groupName, numericalId, KEY)
            ?: controller().getGroupProperty(groupName, KEY)
            ?: defaultJoinState()
    }

    suspend fun setJoinStateAtService(groupName: String, numericalId: Int, joinStateName: String): Boolean {
        return controller().updateServiceProperty(groupName, numericalId, KEY, joinStateName)
    }

    suspend fun getJoinStateAtPersistentServer(serverName: String): String {
        return controller().getPersistentServerProperty(serverName, KEY) ?: defaultJoinState()
    }

    suspend fun ensureJoinStateAtPersistentServer(serverName: String): String {
        val existingJoinState = controller().getPersistentServerProperty(serverName, KEY)
        if (!existingJoinState.isNullOrBlank()) {
            return existingJoinState
        }

        val fallbackState = defaultJoinState()
        if (!setJoinStateAtPersistentServer(serverName, fallbackState)) {
            logger.warning("Could not persist default join state '$fallbackState' for persistent server '$serverName'")
        }
        return fallbackState
    }

    suspend fun setJoinStateAtPersistentServer(serverName: String, joinStateName: String): Boolean {
        val updated = controller().updatePersistentServerProperty(serverName, KEY, joinStateName)
        if (updated) {
            syncLocalStateWithPersistentServer(serverName, joinStateName)
        }
        return updated
    }

    suspend fun setJoinStateAtGroupAndAllServicesInGroup(groupName: String, joinStateName: String): Boolean {
        val groupPropertyUpdated = setJoinStateAtGroup(groupName, joinStateName)
        val servicePropertiesUpdated =
            controller().updateServicePropertyOnAllGroupServers(groupName, KEY, joinStateName)

        val successful = groupPropertyUpdated && servicePropertiesUpdated
        if (!successful) {
            logger.severe("Error setting join state at group and all services in group $groupName.")
        }
        return successful
    }

    fun startGroupStateSyncTask() {
        if (syncJob?.isActive == true) {
            return
        }

        syncJob = scope.launch {
            while (isActive) {
                try {
                    syncLocalStateWithGroupState()
                } catch (e: Exception) {
                    logger.severe("Error while syncing local/group join state: ${e.message}")
                }
                delay(2000.milliseconds)
            }
        }
    }

    fun registerListener() {
        plugin.api.event().server().onUpdated { event ->
            if (event.serverId != SimpleCloudRuntime.serverId()) return@onUpdated

            val server = event.server
            val state = server.properties?.get(KEY)?.toString()

            when {
                state.isNullOrBlank() -> {
                    logger.warning("No join state found for server. Using default join state.")
                    scope.launch { applyDefaultJoinStateForCurrentServer(server) }
                }

                state != localState -> {
                    localState = state
                    logger.info("Join state changed to $state")
                }
            }
        }
    }

    private fun controller() = plugin.cloudControllerHandler

    private fun defaultJoinState(): String = plugin.config.get().initialState

    private suspend fun syncLocalStateWithPersistentServer(serverName: String, joinStateName: String) {
        val currentServer = controller().currentServer ?: return
        if (currentServer.isFromGroup || currentServer.persistentServer?.name != serverName) {
            return
        }

        val synchronized = controller().updateServerProperty(currentServer.serverId, KEY, joinStateName)
        if (synchronized) {
            localState = joinStateName
        }
    }

    private suspend fun applyDefaultJoinStateForCurrentServer(server: Server) {
        try {
            val persistentServerName = server.persistentServer?.name
            val stateToApply = when {
                server.isFromGroup -> ensureJoinStateAtGroup(server.group?.name ?: return)
                persistentServerName != null -> ensureJoinStateAtPersistentServer(persistentServerName)
                else -> defaultJoinState()
            }

            controller().updateServerProperty(server.serverId, KEY, stateToApply)
            localState = stateToApply
        } catch (e: Exception) {
            logger.severe("Error setting default join state: ${e.message}")
        }
    }

    private suspend fun syncLocalStateWithGroupState() {
        val server = controller().currentServer ?: return
        if (!server.isFromGroup) return

        val groupName = server.group?.name ?: return
        val numericalId = server.numericalId

        val groupState = controller().getGroupProperty(groupName, KEY)
        if (groupState.isNullOrBlank()) return

        val previousGroupState = lastObservedGroupState
        if (previousGroupState == null) {
            lastObservedGroupState = groupState
            return
        }

        val serviceState = controller().getServiceProperty(groupName, numericalId, KEY)
        if (serviceState.isNullOrBlank()) {
            applyGroupState(server.serverId, "$groupName-$numericalId", groupState, "service state was missing")
            return
        }

        if (groupState == previousGroupState) return
        lastObservedGroupState = groupState

        if (serviceState != previousGroupState) return
        applyGroupState(server.serverId, "$groupName-$numericalId", groupState, "of group state change")
    }

    private suspend fun applyGroupState(serverId: String, serviceName: String, groupState: String, reason: String) {
        if (!controller().updateServerProperty(serverId, KEY, groupState)) {
            logger.warning("Could not synchronize join state for '$serviceName' with group state '$groupState'")
            return
        }

        localState = groupState
        logger.info("Join state changed to $groupState because $reason.")
    }

    companion object {
        const val KEY = "joinstate"
    }

}
