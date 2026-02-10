package app.simplecloud.plugin.proxy.shared.handler

import app.simplecloud.api.server.Server
import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import kotlinx.coroutines.*
import java.util.logging.Logger

class JoinStateHandler(
    private val proxyPlugin: ProxyPlugin
) {

    private val logger = Logger.getLogger(JoinStateHandler::class.java.name)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var groupStateSyncJob: Job? = null

    @Volatile
    var localState: String = proxyPlugin.joinStateConfiguration.get().defaultState

    companion object {
        const val JOINSTATE_KEY = "joinstate"
    }

    fun stop() {
        groupStateSyncJob?.cancel()
        scope.cancel()
    }

    private fun defaultJoinState(): String = proxyPlugin.joinStateConfiguration.get().defaultState

    suspend fun setJoinStateAtGroup(groupName: String, joinStateName: String): Boolean {
        return this.proxyPlugin.cloudControllerHandler.updateGroupProperty(groupName, JOINSTATE_KEY, joinStateName)
    }

    suspend fun getJoinStateAtGroup(groupName: String): String {
        return proxyPlugin.cloudControllerHandler.getGroupProperty(groupName, JOINSTATE_KEY)
            ?: defaultJoinState()
    }

    suspend fun ensureJoinStateAtGroup(groupName: String): String {
        val existingJoinState = proxyPlugin.cloudControllerHandler.getGroupProperty(groupName, JOINSTATE_KEY)
        if (!existingJoinState.isNullOrBlank()) {
            return existingJoinState
        }
        val fallbackState = defaultJoinState()
        if (!setJoinStateAtGroup(groupName, fallbackState)) {
            logger.warning("Could not persist default join state '$fallbackState' for group '$groupName'")
        }
        return fallbackState
    }

    suspend fun setJoinStateAtService(groupName: String, numericalId: Int, joinStateName: String): Boolean {
        return this.proxyPlugin.cloudControllerHandler.updateServiceProperty(
            groupName,
            numericalId,
            JOINSTATE_KEY,
            joinStateName
        )
    }

    suspend fun getJoinStateAtService(groupName: String, numericalId: Int): String {
        return proxyPlugin.cloudControllerHandler.getServiceProperty(groupName, numericalId, JOINSTATE_KEY)
            ?: defaultJoinState()
    }

    suspend fun ensureJoinStateAtService(groupName: String, numericalId: Int): String {
        val existingJoinState = proxyPlugin.cloudControllerHandler.getServiceProperty(groupName, numericalId, JOINSTATE_KEY)
        if (!existingJoinState.isNullOrBlank()) {
            return existingJoinState
        }
        val fallbackState = defaultJoinState()
        if (!setJoinStateAtService(groupName, numericalId, fallbackState)) {
            logger.warning("Could not persist default join state '$fallbackState' for service '$groupName-$numericalId'")
        }
        return fallbackState
    }

    suspend fun setJoinStateAtGroupAndAllServicesInGroup(groupName: String, joinStateName: String): Boolean {
        val groupPropertyUpdated = setJoinStateAtGroup(groupName, joinStateName)
        val servicePropertiesUpdated = this.proxyPlugin.cloudControllerHandler.updateServicePropertyOnAllGroupServers(
            groupName,
            JOINSTATE_KEY,
            joinStateName
        )
        val successful = groupPropertyUpdated && servicePropertiesUpdated
        if (!successful) {
            logger.severe("Error setting join state at group and all services in group $groupName.")
        }
        return successful
    }

    fun startGroupStateSyncTask() {
        if (groupStateSyncJob?.isActive == true) {
            return
        }
        groupStateSyncJob = scope.launch {
            while (isActive) {
                try {
                    syncLocalStateWithGroupState()
                } catch (e: Exception) {
                    logger.severe("Error while syncing local/group join state: ${e.message}")
                }
                delay(2000)
            }
        }
    }

    fun registerListener() {
        proxyPlugin.api.event().server().onUpdated { event ->
            if (event.serverId != System.getenv("SIMPLECLOUD_UNIQUE_ID")) return@onUpdated

            val server = event.server
            val state = server.properties?.get(JOINSTATE_KEY)?.toString()

            if (state.isNullOrBlank()) {
                this.logger.warning("No join state found for server. Using default join state.")

                scope.launch { applyDefaultJoinStateForCurrentServer(server) }
                return@onUpdated
            }

            if (state == localState) {
                return@onUpdated
            }

            localState = state.toString()
            this.logger.info("Join state changed to $state")
        }
    }

    private suspend fun applyDefaultJoinStateForCurrentServer(server: Server) {
        val fallbackState = defaultJoinState()
        try {
            if (server.isFromGroup) {
                val groupName = server.group?.name ?: return
                proxyPlugin.cloudControllerHandler.updateServerProperty(server.serverId, JOINSTATE_KEY, fallbackState)
                if (proxyPlugin.cloudControllerHandler.getGroupProperty(groupName, JOINSTATE_KEY).isNullOrBlank()) {
                    setJoinStateAtGroup(groupName, fallbackState)
                }
            } else {
                proxyPlugin.cloudControllerHandler.setCurrentServerProperty(JOINSTATE_KEY, fallbackState)
            }
            localState = fallbackState
        } catch (e: Exception) {
            logger.severe("Error setting default join state: ${e.message}")
        }
    }

    private suspend fun syncLocalStateWithGroupState() {
        val server = proxyPlugin.cloudControllerHandler.currentServer ?: return
        if (!server.isFromGroup) return

        val groupName = server.group?.name ?: return
        val numericalId = server.numericalId

        val groupState = proxyPlugin.cloudControllerHandler.getGroupProperty(groupName, JOINSTATE_KEY)
        if (groupState.isNullOrBlank()) {
            return
        }

        if (groupState == localState) {
            return
        }

        val serviceState = proxyPlugin.cloudControllerHandler.getServiceProperty(groupName, numericalId, JOINSTATE_KEY)
        if (serviceState.isNullOrBlank()) {
            val synchronized = proxyPlugin.cloudControllerHandler.updateServerProperty(server.serverId, JOINSTATE_KEY, groupState)
            if (!synchronized) {
                logger.warning("Could not synchronize missing service join state for '$groupName-$numericalId'")
                return
            }
            localState = groupState
            logger.info("Join state changed to $groupState because service state was missing.")
            return
        }

        if (serviceState != localState) {
            return
        }

        localState = groupState
        logger.info("Join state changed to $groupState because of group state change.")
    }
}
