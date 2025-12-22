package app.simplecloud.plugin.proxy.shared.handler

import app.simplecloud.api.server.Server
import app.simplecloud.api.server.ServerQuery
import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import java.util.logging.Logger

class CloudControllerHandler(
    private val plugin: ProxyPlugin,
    private val joinStateHandler: JoinStateHandler
) {
    private val logger = Logger.getLogger(CloudControllerHandler::class.java.name)

    var groupName: String? = null
    var numericalId: Int? = null

    init {
        initializeGroupName()
        joinStateHandler.registerListener()
    }

    private fun initializeGroupName() {
        val serviceID = System.getenv("SIMPLECLOUD_UNIQUE_ID")

        if (serviceID == null) {
            logger.warning("Environment variable SIMPLECLOUD_UNIQUE_ID is not set.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val service = plugin.api.server().getServerById(serviceID).await()

                groupName = service.group.name
                numericalId = service.numericalId
                logger.info("Group name initialized to: $groupName")

                joinStateHandler.localState = joinStateHandler.getJoinStateAtService(
                    groupName!!,
                    numericalId!!
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
        return retrievePropertyOrEmpty {
            plugin.api.group().getGroupByName(groupName).await().properties[key].toString()
        }
    }

    suspend fun getByNumericalId(groupName: String, numericalId: Int) =
        plugin.api.server().getAllServers(ServerQuery.create()
            .filterByServerGroupName(groupName)
            .filterByNumericalId(numericalId)
        ).await()?.firstOrNull()

    suspend fun getByGroup(groupName: String): List<Server> =
        plugin.api.server().getAllServers(ServerQuery.create()
            .filterByServerGroupName(groupName)
        ).await() ?: emptyList()

    suspend fun getServiceProperties(groupName: String, numericalId: Int, key: String): String {
        val server = getByNumericalId(groupName, numericalId)
        if (server == null) {
            logger.severe("Server not found for group '$groupName' and numerical ID '$numericalId'")
            return ""
        }
        return retrievePropertyOrEmpty {
            server.properties[key].toString()
        }
    }

    suspend fun setServiceProperties(groupName: String, numericalId: Int, key: String, value: String): Boolean {
        val server = getByNumericalId(groupName, numericalId)
        if (server == null) {
            logger.severe("Server not found for group '$groupName' and numerical ID '$numericalId'")
            return false
        }
        return try {
            plugin.api.server().updateServerProperties(server.serverId, mapOf(key to value)).await()
            logger.info("Service property '$key' updated to '$value' for service ${server.group} ${server.numericalId} ${server.serverId}")
            true
        } catch (e: Exception) {
            logger.severe("Error updating service properties: ${e.message}")
            false
        }
    }

    suspend fun setServicePropertiesOnAllGroupServices(groupName: String, key: String, value: String): Boolean {
        return try {
            val servers = getByGroup(groupName)
            servers.forEach { server ->
                plugin.api.server().updateServerProperties(server.serverId, mapOf(key to value)).await()
            }
            logger.info("Service property '$key' updated to '$value' for all services in group '$groupName'")
            true
        } catch (e: Exception) {
            logger.severe("Error updating service properties on all group services: ${e.message}")
            false
        }
    }

    suspend fun setGroupProperties(groupName: String, key: String, value: String): Boolean {
        try {
            val groupId = plugin.api.group().getGroupByName(groupName).await().serverGroupId
            plugin.api.group().updateGroupProperties(groupId, mapOf(key to value)).await()
            logger.info("Group property '$key' updated to '$value' for group '$groupName'")
            return true
        } catch (e: Exception) {
            logger.severe("Error updating group properties: ${e.message}")
            return false
        }
    }

    suspend fun getOnlinePlayersInGroup(groupName: String): Int {
        return try {
            getByGroup(groupName).sumOf { it.playerCount.toInt() }
        } catch (e: Exception) {
            logger.severe("Error retrieving online players in group: ${e.message}")
            0
        }
    }

    suspend fun getMaxPlayersInGroup(groupName: String): Int {
        return try {
            plugin.api.group().getGroupByName(groupName).await().maxPlayers
        } catch (e: Exception) {
            logger.severe("Error retrieving max players in group: ${e.message}")
            0
        }
    }

    suspend fun getAllGroups(): List<String> {
        return plugin.api.group().allGroups.await().map { it.name }
    }

    suspend fun getAllNumericalIdsFromGroup(groupName: String): List<Int> {
        return getByGroup(groupName).map { it.numericalId }
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
