package app.simplecloud.plugin.proxy.shared.handler

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import java.util.logging.Logger

class JoinStateHandler(
    private val proxyPlugin: ProxyPlugin
) {

    private val logger = Logger.getLogger(JoinStateHandler::class.java.name)

    var localState: String = ""

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
}