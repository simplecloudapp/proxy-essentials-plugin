package app.simplecloud.plugin.proxy.shared.handler

import app.simplecloud.api.persistentserver.PersistentServer
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val serviceId = System.getenv("SIMPLECLOUD_UNIQUE_ID")

    @Volatile
    var currentServer: Server? = null
        private set

    init {
        initialize()
    }

    private fun initialize() {
        if (serviceId.isNullOrBlank()) {
            logger.warning("Environment variable SIMPLECLOUD_UNIQUE_ID is not set.")
            joinStateHandler.registerListener()
            plugin.motdLayoutHandler.registerListener()
            return
        }

        scope.launch {
            try {
                currentServer = plugin.api.server().getServerById(serviceId).await()
                logger.info("Initialized server: ${currentServer?.serverBase?.name}")

                joinStateHandler.registerListener()
                plugin.motdLayoutHandler.registerListener()
                initializeJoinState()
            } catch (e: Exception) {
                logger.severe("Error retrieving server by ID: ${e.message}")
                joinStateHandler.registerListener()
                plugin.motdLayoutHandler.registerListener()
            }
        }
    }

    private suspend fun initializeJoinState() {
        val server = currentServer ?: return

        if (server.isFromGroup) {
            val groupName = server.group?.name ?: return
            joinStateHandler.localState = joinStateHandler.getJoinStateAtService(groupName, server.numericalId)
            joinStateHandler.startGroupStateSyncTask()
            plugin.motdLayoutHandler.setLocalLayout(
                getServiceProperty(groupName, server.numericalId, MotdLayoutHandler.MOTD_LAYOUT_KEY)
                    ?: getGroupProperty(groupName, MotdLayoutHandler.MOTD_LAYOUT_KEY)
            )
        } else {
            val joinState = server.properties?.get(JoinStateHandler.JOINSTATE_KEY)?.toString()
            if (!joinState.isNullOrEmpty()) {
                joinStateHandler.localState = joinState
            } else {
                val persistentName = server.persistentServer?.name
                if (persistentName != null) {
                    joinStateHandler.localState = joinStateHandler.ensureJoinStateAtPersistentServer(persistentName)
                }
            }
            plugin.motdLayoutHandler.setLocalLayout(server.properties?.get(MotdLayoutHandler.MOTD_LAYOUT_KEY)?.toString())
        }
    }

    fun close() {
        scope.cancel()
    }

    suspend fun updateServerProperty(serverId: String, key: String, value: String): Boolean {
        return try {
            plugin.api.server().updateServerProperties(serverId, mapOf(key to value)).await()
            true
        } catch (e: Exception) {
            logger.severe("Error updating server property: ${e.message}")
            false
        }
    }

    suspend fun getGroupProperty(groupName: String, key: String): String? {
        return try {
            getGroupByName(groupName)?.properties?.get(key)?.toString()?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            logger.severe("Error retrieving group property: ${e.message}")
            null
        }
    }

    private suspend fun getGroupByName(groupName: String) = try {
        plugin.api.group().getGroupByName(groupName).await()
            ?: findGroupCaseInsensitive(groupName)
    } catch (_: Exception) {
        try {
            findGroupCaseInsensitive(groupName)
        } catch (e2: Exception) {
            logger.severe("Error retrieving group '$groupName': ${e2.message}")
            null
        }
    }

    private suspend fun findGroupCaseInsensitive(groupName: String) =
        plugin.api.group().allGroups.await().firstOrNull { it.name.equals(groupName, ignoreCase = true) }

    private suspend fun resolveGroupNameCasing(groupName: String): String =
        getGroupByName(groupName)?.name ?: groupName

    suspend fun groupExists(groupName: String): Boolean {
        return getGroupByName(groupName) != null
    }

    suspend fun getPersistentServerByName(name: String): PersistentServer? = try {
        plugin.api.persistentServer().getPersistentServerByName(name).await()
            ?: findPersistentServerCaseInsensitive(name)
    } catch (_: Exception) {
        try {
            findPersistentServerCaseInsensitive(name)
        } catch (e2: Exception) {
            logger.severe("Error retrieving persistent server '$name': ${e2.message}")
            null
        }
    }

    private suspend fun findPersistentServerCaseInsensitive(name: String) =
        plugin.api.persistentServer().allPersistentServers.await().firstOrNull { it.name.equals(name, ignoreCase = true) }

    suspend fun getPersistentServerProperty(name: String, key: String): String? {
        return try {
            getPersistentServerByName(name)?.properties?.get(key)?.toString()?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            logger.severe("Error retrieving persistent server property: ${e.message}")
            null
        }
    }

    suspend fun updatePersistentServerProperty(name: String, key: String, value: String): Boolean {
        val persistentServer = getPersistentServerByName(name) ?: return false
        return try {
            plugin.api.persistentServer().updatePersistentServerProperty(persistentServer.persistentServerId, key, value).await()
            logger.info("Persistent server property '$key' updated to '$value' for persistent server '$name'")
            true
        } catch (e: Exception) {
            logger.severe("Error updating persistent server property: ${e.message}")
            false
        }
    }

    suspend fun getAllPersistentServerNames(): List<String> {
        return try {
            plugin.api.persistentServer().allPersistentServers.await().map { it.name }
        } catch (e: Exception) {
            logger.severe("Error retrieving persistent servers: ${e.message}")
            emptyList()
        }
    }

    suspend fun getServerByNumericalId(groupName: String, numericalId: Int): Server? = try {
        plugin.api.server().getAllServers(
            ServerQuery.create()
                .filterByServerGroupName(resolveGroupNameCasing(groupName))
                .filterByNumericalId(numericalId)
        ).await()?.firstOrNull()
    } catch (e: Exception) {
        logger.severe("Error retrieving server '$groupName-$numericalId': ${e.message}")
        null
    }

    suspend fun getServersByGroup(groupName: String): List<Server> = try {
        plugin.api.server().getAllServers(
            ServerQuery.create()
                .filterByServerGroupName(resolveGroupNameCasing(groupName))
        ).await() ?: emptyList()
    } catch (e: Exception) {
        logger.severe("Error retrieving servers for group '$groupName': ${e.message}")
        emptyList()
    }

    suspend fun getServiceProperty(groupName: String, numericalId: Int, key: String): String? {
        val server = getServerByNumericalId(groupName, numericalId) ?: return null
        return server.properties[key]?.toString()?.takeIf { it.isNotBlank() }
    }

    suspend fun updateServiceProperty(groupName: String, numericalId: Int, key: String, value: String): Boolean {
        val server = getServerByNumericalId(groupName, numericalId)
        if (server == null) {
            logger.severe("Server not found for group '$groupName' and numerical ID '$numericalId'")
            return false
        }
        return updateServerProperty(server.serverId, key, value)
    }

    suspend fun updateServicePropertyOnAllGroupServers(groupName: String, key: String, value: String): Boolean {
        val servers = getServersByGroup(groupName)
        var allSuccessful = true
        servers.forEach { server ->
            if (!updateServerProperty(server.serverId, key, value)) {
                allSuccessful = false
            }
        }
        if (allSuccessful) {
            logger.info("Service property '$key' updated to '$value' for all services in group '$groupName'")
        }
        return allSuccessful
    }

    suspend fun updateGroupProperty(groupName: String, key: String, value: String): Boolean {
        val group = getGroupByName(groupName) ?: return false
        try {
            plugin.api.group().updateGroupProperties(group.serverGroupId, mapOf(key to value)).await()
            logger.info("Group property '$key' updated to '$value' for group '$groupName'")
            return true
        } catch (e: Exception) {
            logger.severe("Error updating group properties: ${e.message}")
            return false
        }
    }

    suspend fun getOnlinePlayersInGroup(groupName: String): Int {
        return try {
            getServersByGroup(groupName).sumOf { it.playerCount ?: 0 }
        } catch (e: Exception) {
            logger.severe("Error retrieving online players in group: ${e.message}")
            0
        }
    }

    suspend fun getMaxPlayersInGroup(groupName: String): Int {
        return getGroupByName(groupName)?.maxPlayers ?: 0
    }

    suspend fun getAllGroups(): List<String> {
        return try {
            plugin.api.group().allGroups.await().map { it.name }
        } catch (e: Exception) {
            logger.severe("Error retrieving groups: ${e.message}")
            emptyList()
        }
    }

    suspend fun getAllNumericalIdsFromGroup(groupName: String): List<Int> {
        return getServersByGroup(groupName).map { it.numericalId }
    }

    suspend fun getServerByName(name: String): Server? {
        return try {
            val servers = plugin.api.server().getAllServers(ServerQuery.create()).await() ?: return null
            servers.find { it.serverBase.name == name || it.persistentServer?.name == name }
                ?: servers.find {
                    it.serverBase.name.equals(name, ignoreCase = true)
                            || it.persistentServer?.name.equals(name, ignoreCase = true)
                }
        } catch (e: Exception) {
            logger.severe("Error retrieving server by name: ${e.message}")
            null
        }
    }

    suspend fun getPersistentServersByNames(names: Set<String>): List<Server> {
        if (names.isEmpty()) {
            return emptyList()
        }

        return try {
            val servers = plugin.api.server().getAllServers(ServerQuery.create()).await() ?: return emptyList()
            servers.filter { server -> server.persistentServer?.name?.let { it in names } == true }
        } catch (e: Exception) {
            logger.severe("Error retrieving persistent servers by name: ${e.message}")
            emptyList()
        }
    }
}
