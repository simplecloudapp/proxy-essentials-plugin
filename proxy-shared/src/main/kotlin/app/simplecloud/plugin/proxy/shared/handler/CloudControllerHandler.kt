package app.simplecloud.plugin.proxy.shared.handler

import app.simplecloud.controller.api.ControllerApi
import kotlinx.coroutines.*
import java.util.logging.Logger

class CloudControllerHandler(
    private val joinStateHandler: JoinStateHandler
) {

    private val logger = Logger.getLogger(CloudControllerHandler::class.java.name)
    private val controllerApi = createControllerApiOrNull()

    var groupName: String? = null
    var numericalId: Int? = null

    fun isControllerAvailable(): Boolean {
        return controllerApi != null
    }

    init {
        initializeGroupName()
    }

    private fun createControllerApiOrNull(): ControllerApi.Coroutine? {
        return try {
            ControllerApi.createCoroutineApi()
        } catch (e: Throwable) {
            logger.warning("ControllerApi is not available: ${e.message}")
            null
        }
    }

    private fun initializeGroupName() {
        val api = controllerApi
        if (api == null) {
            logger.warning("Skipping controller initialization because ControllerApi is unavailable.")
            return
        }

        val serviceID = System.getenv("SIMPLECLOUD_UNIQUE_ID")

        if (serviceID == null) {
            logger.warning("Environment variable SIMPLECLOUD_UNIQUE_ID is not set.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val service = api.getServers().getServerById(serviceID)
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
        val api = controllerApi ?: return ""
        return groupName.let {
            retrievePropertyOrEmpty {
                api.getGroups().getGroupByName(it).properties[key]
            }
        }
    }

    suspend fun getServiceProperties(groupName: String, numericalId: Long, key: String): String {
        val api = controllerApi ?: return ""
        return api.getServers().getServerByNumerical(groupName, numericalId).let { server ->
            retrievePropertyOrEmpty {
                server.properties[key]
            }
        }
    }

    suspend fun setServiceProperties(groupName: String, numericalId: Long, key: String, value: String): Boolean {
        val api = controllerApi ?: return false
        api.getServers().getServerByNumerical(groupName, numericalId).let { server ->
            try {
                api.getServers().updateServerProperty(server.uniqueId, key, value)
                logger.info("Service property '$key' updated to '$value'")
                return true
            } catch (e: Exception) {
                logger.severe("Error updating service properties: ${e.message}")
                return false
            }
        }
    }

    suspend fun setServicePropertiesOnAllGroupServices(groupName: String, key: String, value: String): Boolean {
        val api = controllerApi ?: return false
        groupName.let { name ->
            try {
                api.getServers().getServersByGroup(name).forEach { server ->
                    logger.info("Updating service property '$key' to '$value' on service ${server.group} ${server.numericalId} ${server.uniqueId}")
                    api.getServers().updateServerProperty(server.uniqueId, key, value)
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
        val api = controllerApi ?: return false
        groupName.let { name ->
            try {
                val group = api.getGroups().getGroupByName(name)
                val updatedGroup = group.copy(properties = group.properties + (key to value))
                api.getGroups().updateGroup(updatedGroup)
                logger.info("Group property '$key' updated to '$value'")
                return true
            } catch (e: Exception) {
                logger.severe("Error updating group properties: ${e.message}")
                return false
            }
        }
    }

    suspend fun getOnlinePlayersInGroup(groupName: String): Int {
        val api = controllerApi ?: return 0
        return groupName.let { name ->
            try {
                api.getServers().getServersByGroup(name).sumOf { it.playerCount.toInt() }
            } catch (e: Exception) {
                logger.severe("Error retrieving online players in group: ${e.message}")
                0
            }
        }

    }

    suspend fun getMaxPlayersInGroup(groupName: String): Int {
        val api = controllerApi ?: return 0
        return groupName.let {
            try {
                api.getGroups().getGroupByName(it).maxPlayers.toInt()
            } catch (e: Exception) {
                logger.severe("Error retrieving max players in group: ${e.message}")
                0
            }
        }
    }

    suspend fun getAllGroups(): List<String> {
        val api = controllerApi ?: return emptyList()
        return api.getGroups().getAllGroups().map { it.name }
    }

    suspend fun getAllNumericalIdsFromGroup(groupName: String): List<Int> {
        val api = controllerApi ?: return emptyList()
        return api.getServers().getServersByGroup(groupName).map { it.numericalId }
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
