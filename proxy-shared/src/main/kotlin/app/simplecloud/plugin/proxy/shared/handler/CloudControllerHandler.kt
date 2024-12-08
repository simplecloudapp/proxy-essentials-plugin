package app.simplecloud.plugin.proxy.shared.handler

import app.simplecloud.controller.api.ControllerApi
import kotlinx.coroutines.runBlocking
import java.util.logging.Logger

class CloudControllerHandler {

    private val controllerApi = ControllerApi.createCoroutineApi()
    private var groupName: String? = null
    private val logger = Logger.getLogger(CloudControllerHandler::class.java.name)

    init {
        initializeGroupName()
    }

    private fun initializeGroupName() {
        val uniqueId = System.getenv("SIMPLECLOUD_UNIQUE_ID")

        if (uniqueId == null) {
            logger.warning("Environment variable SIMPLECLOUD_UNIQUE_ID is not set.")
            return
        }

        runBlocking {
            try {
                val service = controllerApi.getServers().getServerById(uniqueId)
                groupName = service.group
                logger.info("Group name initialized to: $groupName")
            } catch (e: Exception) {
                logger.severe("Error retrieving server by ID: ${e.message}")
            }
        }
    }

    suspend fun getServiceProperties(key: String): String {
        return retrievePropertyOrEmpty {
            val uniqueId = System.getenv("SIMPLECLOUD_UNIQUE_ID")
            controllerApi.getServers().getServerById(uniqueId).properties[key]
        }
    }

    suspend fun getGroupProperties(key: String): String {
        return groupName?.let {
            retrievePropertyOrEmpty {
                controllerApi.getGroups().getGroupByName(it).properties[key]
            }
        } ?: run {
            logger.warning("Group name is not initialized.")
            ""
        }
    }

    suspend fun setServiceProperties(key: String, value: String) {
        val uniqueId = System.getenv("SIMPLECLOUD_UNIQUE_ID")

        try {
            controllerApi.getServers().updateServerProperty(uniqueId, key, value)
            logger.info("Service property '$key' updated to '$value'")
        } catch (e: Exception) {
            logger.severe("Error updating service properties: ${e.message}")
        }
    }

    suspend fun setServicePropertiesOnAllGroupServices(key: String, value: String) {
        groupName?.let { name ->
            try {
                controllerApi.getServers().getServersByGroup(name).forEach { server ->
                    controllerApi.getServers().updateServerProperty(server.uniqueId, key, value)
                }
                logger.info("Service property '$key' updated to '$value' on all services in group '$name'")
            } catch (e: Exception) {
                logger.severe("Error updating service properties on all group services: ${e.message}")
            }
        } ?: logger.warning("Group name is not initialized.")
    }

    suspend fun setGroupProperties(key: String, value: String) {
        groupName?.let { name ->
            try {
                val group = controllerApi.getGroups().getGroupByName(name)
                val updatedGroup = group.copy(properties = group.properties + (key to value))
                controllerApi.getGroups().updateGroup(updatedGroup)
                logger.info("Group property '$key' updated to '$value'")
            } catch (e: Exception) {
                logger.severe("Error updating group properties: ${e.message}")
            }
        } ?: logger.warning("Group name is not initialized.")
    }

    suspend fun getOnlinePlayersInGroup(): Int {
        return groupName?.let {
            try {
                controllerApi.getServers().getServersByGroup(it).sumBy { it.playerCount.toInt() }
            } catch (e: Exception) {
                logger.severe("Error retrieving online players in group: ${e.message}")
                0
            }
        } ?: run {
            logger.warning("Group name is not initialized.")
            0
        }
    }

    suspend fun getMaxPlayersInGroup(): Int {
        return groupName?.let {
            try {
                controllerApi.getGroups().getGroupByName(it).maxPlayers.toInt()
            } catch (e: Exception) {
                logger.severe("Error retrieving max players in group: ${e.message}")
                0
            }
        } ?: run {
            logger.warning("Group name is not initialized.")
            0
        }
    }

    private suspend fun retrievePropertyOrEmpty(retrieve: suspend () -> String?): String {
        return try {
            retrieve() ?: ""
        } catch (e: Exception) {
            logger.severe("Error retrieving property: ${e.message}")
            ""
        }
    }
}
