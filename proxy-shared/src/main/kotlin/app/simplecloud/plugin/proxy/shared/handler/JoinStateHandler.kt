package app.simplecloud.plugin.proxy.shared.handler

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import kotlinx.coroutines.*
import java.util.logging.Logger

class JoinStateHandler(
    private val proxyPlugin: ProxyPlugin
) {

    private val logger = Logger.getLogger(JoinStateHandler::class.java.name)

    var localState: String = proxyPlugin.joinStateConfiguration.defaultState

    companion object {
        val JOINSTATE_KEY = "joinstate"
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

        if (groupProperties.isEmpty()) {
            logger.warning("No join state found for group $groupName. Using default join state.")
            setJoinStateAtGroup(groupName, this.proxyPlugin.joinStateConfiguration.defaultState)

            return getJoinStateAtGroup(groupName)
        }

        return groupProperties
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
    suspend fun setJoinStateAtService(groupName: String, numericalId: Long, joinStateName: String): Boolean {
        return this.proxyPlugin.cloudControllerHandler.setServiceProperties(groupName, numericalId, JOINSTATE_KEY, joinStateName)
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
    suspend fun getJoinStateAtService(groupName: String, numericalId: Long): String {
        val serviceProperties = this.proxyPlugin.cloudControllerHandler.getServiceProperties(groupName, numericalId, JOINSTATE_KEY)

        if (serviceProperties.isEmpty()) {
            logger.warning("No join state found for service $numericalId in group $groupName. Using default join state.")
            setJoinStateAtService(groupName, numericalId, this.proxyPlugin.joinStateConfiguration.defaultState)

            return getJoinStateAtService(groupName, numericalId)
        }

        return serviceProperties
    }

    fun startCheckGroupStateTask() {
        CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                getGroupState()
                delay(2000)
            }
        }
    }

    private suspend fun getGroupState() {
        val cloudControllerHandler = this.proxyPlugin.cloudControllerHandler

        if (cloudControllerHandler.groupName == null) {
            logger.warning("Group name is not initialized.")
            return
        }

        val state = cloudControllerHandler.getGroupProperties(cloudControllerHandler.groupName!!, JOINSTATE_KEY)

        if (state.isEmpty()) {
            logger.warning("No join state found for group ${cloudControllerHandler.groupName}. Using default join state.")
            setJoinStateAtGroup(cloudControllerHandler.groupName!!, this.proxyPlugin.joinStateConfiguration.defaultState)
            return
        }

        if (state != localState) {
            if (getJoinStateAtService(cloudControllerHandler.groupName!!, cloudControllerHandler.numericalId!!.toLong()) != localState) {
                //Skip group state change if service state is different
                //logger.info("Join state not changed to $state because of different service state.")
                return
            }

            localState = state
            logger.info("Join state changed to $state because of group state change.")
        }
    }
}