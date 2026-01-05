package app.simplecloud.plugin.proxy.shared.handler

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import kotlinx.coroutines.*
import java.util.logging.Logger

class JoinStateHandler(
    private val proxyPlugin: ProxyPlugin
) {

    private val logger = Logger.getLogger(JoinStateHandler::class.java.name)

    @Volatile
    var localState: String = proxyPlugin.joinStateConfiguration.get().defaultState

    companion object {
        const val JOINSTATE_KEY = "joinstate"
    }

    /**
     * Sets the join state of a group.
     *
     * @param groupName The name of the group.
     * @param joinStateName The name of the join state.
     */
    suspend fun setJoinStateAtGroup(groupName: String, joinStateName: String) {
        this.proxyPlugin.cloudControllerHandler.setGroupProperties(groupName, JOINSTATE_KEY, joinStateName)
    }

    /**
     * Gets the join state of a group.
     *
     * @param groupName The name of the group.
     * @return The name of the join state.
     */
    suspend fun getJoinStateAtGroup(groupName: String): String {
        val groupProperties = this.proxyPlugin.cloudControllerHandler.getGroupProperties(groupName, JOINSTATE_KEY)

        if (groupProperties.isNotEmpty()) {
            return groupProperties
        }

        val defaultState = this.proxyPlugin.joinStateConfiguration.get().defaultState
        logger.warning("No join state found for group $groupName. Setting default: $defaultState")
        setJoinStateAtGroup(groupName, defaultState)
        return defaultState
    }

    /**
     * Sets the join state of a service.
     *
     * @param groupName The name of the group.
     * @param numericalId The numerical id of the service.
     * @param joinStateName The name of the join state.
     *
     * @return True if the join state was set successfully, false otherwise.
     */
    suspend fun setJoinStateAtService(groupName: String, numericalId: Int, joinStateName: String): Boolean {
        return this.proxyPlugin.cloudControllerHandler.setServiceProperties(
            groupName,
            numericalId,
            JOINSTATE_KEY,
            joinStateName
        )
    }

    /**
     * Sets the join state of a group and all services in the group.
     *
     * @param groupName The name of the group.
     * @param joinStateName The name of the join state.
     *
     * @return True if the join state was set successfully, false otherwise.
     */
    suspend fun setJoinStateAtGroupAndAllServicesInGroup(groupName: String, joinStateName: String): Boolean {
        val groupProperties =
            this.proxyPlugin.cloudControllerHandler.setGroupProperties(groupName, JOINSTATE_KEY, joinStateName)
        val servicePropertiesOnAllGroupServices =
            this.proxyPlugin.cloudControllerHandler.setServicePropertiesOnAllGroupServices(
                groupName,
                JOINSTATE_KEY,
                joinStateName
            )

        if (!groupProperties || !servicePropertiesOnAllGroupServices) {
            logger.severe("Error setting join state at group and all services in group $groupName.")
            return false
        }

        return true
    }

    /**
     * Gets the join state of a service.
     *
     * @param groupName The name of the group.
     * @param numericalId The numerical id of the service.
     * @return The name of the join state.
     */
    suspend fun getJoinStateAtService(groupName: String, numericalId: Int): String {
        val serviceProperties =
            this.proxyPlugin.cloudControllerHandler.getServiceProperties(groupName, numericalId, JOINSTATE_KEY)

        if (serviceProperties.isNotEmpty()) {
            return serviceProperties
        }

        val defaultState = this.proxyPlugin.joinStateConfiguration.get().defaultState
        logger.warning("No join state found for service $numericalId in group $groupName. Setting default: $defaultState")
        setJoinStateAtService(groupName, numericalId, defaultState)
        return defaultState
    }

    fun startCheckGroupStateTask() {
        CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    getGroupState()
                } catch (e: Exception) {
                    logger.severe("Error in check group state task: ${e.message}")
                }
                delay(2000)
            }
        }
    }

    fun registerListener() {
        proxyPlugin.api.event().server().onUpdated { event ->
            if (event.serverId != System.getenv("SIMPLECLOUD_UNIQUE_ID")) return@onUpdated

            val server = event.server
            val state = server.properties?.get(JOINSTATE_KEY)

            if (state == null) {
                this.logger.warning("No join state found for server. Using default join state.")

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val defaultState = proxyPlugin.joinStateConfiguration.get().defaultState

                        // Only update group and all services for group-based servers
                        if (server.isFromGroup) {
                            val groupName = server.group?.name
                            if (groupName != null) {
                                setJoinStateAtGroupAndAllServicesInGroup(groupName, defaultState)
                            }
                        } else {
                            // For persistent servers, just update this server's property
                            proxyPlugin.cloudControllerHandler.setServerProperty(JOINSTATE_KEY, defaultState)
                        }

                        localState = defaultState
                    } catch (e: Exception) {
                        logger.severe("Error setting default join state: ${e.message}")
                    }
                }
                return@onUpdated
            }

            if (state == localState) {
                return@onUpdated
            }

            localState = state.toString()
            this.logger.info("Join state changed to $state")
        }
    }

    private suspend fun getGroupState() {
        val server = proxyPlugin.cloudControllerHandler.currentServer ?: return
        if (!server.isFromGroup) return

        val groupName = server.group?.name ?: return
        val numericalId = server.numericalId

        val state = proxyPlugin.cloudControllerHandler.getGroupProperties(groupName, JOINSTATE_KEY)
        if (state.isEmpty()) {
            logger.warning("No join state found for group $groupName. Using default.")
            setJoinStateAtGroup(groupName, proxyPlugin.joinStateConfiguration.get().defaultState)
            return
        }

        if (state != localState) {
            if (getJoinStateAtService(groupName, numericalId) != localState) return
            localState = state
            logger.info("Join state changed to $state because of group state change.")
        }
    }

}