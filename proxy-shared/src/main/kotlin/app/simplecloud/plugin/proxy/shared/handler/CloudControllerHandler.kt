package app.simplecloud.plugin.proxy.shared.handler

import app.simplecloud.controller.api.ControllerApi
import kotlinx.coroutines.*
import java.util.logging.Logger

class CloudControllerHandler(
    private val joinStateHandler: JoinStateHandler
) {

    private val controllerApi = ControllerApi.createCoroutineApi()
    private val logger = Logger.getLogger(CloudControllerHandler::class.java.name)

    var groupName: String? = null
    var numericalId: Int? = null

    init {
        initializeGroupName()
        joinStateHandler.registerPubSubListener(controllerApi.getPubSubClient())
    }

    private fun initializeGroupName() {
        val serviceID = System.getenv("SIMPLECLOUD_UNIQUE_ID")

        if (serviceID == null) {
            logger.warning("Environment variable SIMPLECLOUD_UNIQUE_ID is not set.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val service = controllerApi.getServers().getServerById(serviceID)
                groupName = service.group
                numericalId = service.numericalId
                logger.info("Group name initialized to: $groupName")

                joinStateHandler.localState = joinStateHandler.getJoinStateAtService(
                    groupName!!,
                    numericalId!!.toLong()
                )

                joinStateHandler.startCheckGroupStateTask()
            } catch (e: Exception) {
                logger.severe("Error retrieving server by ID: ${e.message}")
            }
        }
    }

    /*suspend fun getServiceProperties(key: String): String {
        return retrievePropertyOrEmpty {
            val uniqueId = System.getenv("SIMPLECLOUD_UNIQUE_ID")
            controllerApi.getServers().getServerById(uniqueId).properties[key]
        }
    }*/

    suspend fun getGroupProperties(groupName: String, key: String): String {
        return groupName.let {
            retrievePropertyOrEmpty {
                controllerApi.getGroups().getGroupByName(it).properties[key]
            }
        }
    }

    suspend fun getServiceProperties(groupName: String, numericalId: Long, key: String): String {
        return controllerApi.getServers().getServerByNumerical(groupName, numericalId).let { server ->
            retrievePropertyOrEmpty {
                server.properties[key]
            }
        }
    }

    suspend fun setServiceProperties(groupName: String, numericalId: Long, key: String, value: String): Boolean {
        controllerApi.getServers().getServerByNumerical(groupName, numericalId).let { server ->
            try {
                controllerApi.getServers().updateServerProperty(server.uniqueId, key, value)
                logger.info("Service property '$key' updated to '$value'")
                return true
            } catch (e: Exception) {
                logger.severe("Error updating service properties: ${e.message}")
                return false
            }
        }
    }

    suspend fun setServicePropertiesOnAllGroupServices(groupName: String, key: String, value: String): Boolean {
        groupName.let { name ->
            try {
                controllerApi.getServers().getServersByGroup(name).forEach { server ->
                    logger.info("Updating service property '$key' to '$value' on service ${server.group} ${server.numericalId} ${server.uniqueId}")
                    controllerApi.getServers().updateServerProperty(server.uniqueId, key, value)
                }
                logger.info("Service property '$key' updated to '$value' on all services in group '$name'")
                return true
            } catch (e: Exception) {
                logger.severe("Error updating service properties on all group services: ${e.message}")
                return false
            }
        }
    }

    suspend fun setGroupProperties(groupName: String, key: String, value: String): Boolean {
        groupName.let { name ->
            try {
                val group = controllerApi.getGroups().getGroupByName(name)
                val updatedGroup = group.copy(properties = group.properties + (key to value))
                controllerApi.getGroups().updateGroup(updatedGroup)
                logger.info("Group property '$key' updated to '$value'")
                return true
            } catch (e: Exception) {
                logger.severe("Error updating group properties: ${e.message}")
                return false
            }
        }
    }

    suspend fun getOnlinePlayersInGroup(groupName: String): Int {
        return groupName.let { name ->
            try {
                controllerApi.getServers().getServersByGroup(name).sumOf { it.playerCount.toInt() }
            } catch (e: Exception) {
                logger.severe("Error retrieving online players in group: ${e.message}")
                0
            }
        }

    }

    suspend fun getMaxPlayersInGroup(groupName: String): Int {
        return groupName.let {
            try {
                controllerApi.getGroups().getGroupByName(it).maxPlayers.toInt()
            } catch (e: Exception) {
                logger.severe("Error retrieving max players in group: ${e.message}")
                0
            }
        }
    }

    suspend fun getAllGroups(): List<String> {
        return controllerApi.getGroups().getAllGroups().map { it.name }
    }

    suspend fun getAllNumericalIdsFromGroup(groupName: String): List<Int> {
        return controllerApi.getServers().getServersByGroup(groupName).map { it.numericalId }
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
