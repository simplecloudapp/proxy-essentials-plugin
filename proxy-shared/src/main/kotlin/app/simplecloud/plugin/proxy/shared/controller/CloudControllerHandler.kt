package app.simplecloud.plugin.proxy.shared.controller

import app.simplecloud.api.group.Group
import app.simplecloud.api.persistentserver.PersistentServer
import app.simplecloud.api.runtime.SimpleCloudRuntime
import app.simplecloud.api.server.Server
import app.simplecloud.api.server.ServerQuery
import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import app.simplecloud.plugin.proxy.shared.joinstate.JoinStateService
import app.simplecloud.plugin.proxy.shared.motd.MotdLayoutRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import java.util.logging.Logger

class CloudControllerHandler(
    private val plugin: ProxyPlugin,
    private val service: JoinStateService
) {

    private val logger = Logger.getLogger(CloudControllerHandler::class.java.name)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    var currentServer: Server? = null
        private set

    fun start() {
        if (SimpleCloudRuntime.serverId().isNullOrBlank()) {
            logger.warning("Environment variable SIMPLECLOUD_UNIQUE_ID is not set.")
            registerListeners()
            return
        }

        scope.launch {
            try {
                currentServer = plugin.api.server().getServerById(SimpleCloudRuntime.serverId()).await()
                logger.info("Initialized server: ${currentServer?.serverBase?.name}")

                registerListeners()
                initializeJoinState()
            } catch (e: Exception) {
                logger.severe("Error retrieving server by ID: ${e.message}")
                registerListeners()
            }
        }
    }

    fun close() {
        scope.cancel()
    }

    private fun registerListeners() {
        service.registerListener()
        plugin.layoutRepository.registerListener()
    }

    private suspend fun initializeJoinState() {
        val server = currentServer ?: return

        when {
            server.isFromGroup -> initializeJoinStateOfGroupServer(server)
            else -> initializeJoinStateOfPersistentServer(server)
        }
    }

    private suspend fun initializeJoinStateOfGroupServer(server: Server) {
        val groupName = server.group?.name ?: return

        service.localState = service.getJoinStateAtService(groupName, server.numericalId)
        service.startGroupStateSyncTask()
        plugin.layoutRepository.setLocalLayout(
            getServiceProperty(groupName, server.numericalId, MotdLayoutRepository.KEY)
                ?: getGroupProperty(groupName, MotdLayoutRepository.KEY)
        )
    }

    private suspend fun initializeJoinStateOfPersistentServer(server: Server) {
        val joinState = server.properties?.get(JoinStateService.KEY)?.toString()
        val persistentServerName = server.persistentServer?.name

        when {
            !joinState.isNullOrEmpty() -> service.localState = joinState
            persistentServerName != null ->
                service.localState = service.ensureJoinStateAtPersistentServer(persistentServerName)
        }

        plugin.layoutRepository.setLocalLayout(
            server.properties?.get(MotdLayoutRepository.KEY)?.toString()
        )
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

    suspend fun groupExists(groupName: String): Boolean = getGroupByName(groupName) != null

    suspend fun getGroupProperty(groupName: String, key: String): String? {
        return try {
            getGroupByName(groupName)?.properties?.get(key)?.toString().orNullIfBlank()
        } catch (e: Exception) {
            logger.severe("Error retrieving group property: ${e.message}")
            null
        }
    }

    suspend fun updateGroupProperty(groupName: String, key: String, value: String): Boolean {
        val group = getGroupByName(groupName) ?: return false
        return try {
            plugin.api.group().updateGroupProperties(group.serverGroupId, mapOf(key to value)).await()
            logger.info("Group property '$key' updated to '$value' for group '$groupName'")
            true
        } catch (e: Exception) {
            logger.severe("Error updating group properties: ${e.message}")
            false
        }
    }

    suspend fun getAllGroups(): List<String> {
        return try {
            plugin.api.group().allGroups.await().map { it.name }
        } catch (e: Exception) {
            logger.severe("Error retrieving groups: ${e.message}")
            emptyList()
        }
    }

    suspend fun getMaxPlayersInGroup(groupName: String): Int = getGroupByName(groupName)?.maxPlayers ?: 0

    suspend fun getOnlinePlayersInGroup(groupName: String): Int {
        return try {
            getServersByGroup(groupName).sumOf { it.playerCount ?: 0 }
        } catch (e: Exception) {
            logger.severe("Error retrieving online players in group: ${e.message}")
            0
        }
    }

    suspend fun getAllNumericalIdsFromGroup(groupName: String): List<Int> {
        return getServersByGroup(groupName).map { it.numericalId }
    }

    suspend fun getPersistentServerByName(name: String): PersistentServer? {
        val persistentServer = try {
            plugin.api.persistentServer().getPersistentServerByName(name).await()
        } catch (_: Exception) {
            null
        }
        if (persistentServer != null) {
            return persistentServer
        }

        return try {
            plugin.api.persistentServer().allPersistentServers.await()
                .firstOrNull { it.name.equals(name, ignoreCase = true) }
        } catch (e: Exception) {
            logger.severe("Error retrieving persistent server '$name': ${e.message}")
            null
        }
    }

    suspend fun getPersistentServerProperty(name: String, key: String): String? {
        return try {
            getPersistentServerByName(name)?.properties?.get(key)?.toString().orNullIfBlank()
        } catch (e: Exception) {
            logger.severe("Error retrieving persistent server property: ${e.message}")
            null
        }
    }

    suspend fun updatePersistentServerProperty(name: String, key: String, value: String): Boolean {
        val persistentServer = getPersistentServerByName(name) ?: return false
        return try {
            plugin.api.persistentServer()
                .updatePersistentServerProperty(persistentServer.persistentServerId, key, value)
                .await()
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

    suspend fun getServersByGroup(groupName: String): List<Server> {
        return try {
            plugin.api.server().getAllServers(
                ServerQuery.create().filterByServerGroupName(resolveGroupNameCasing(groupName))
            ).await() ?: emptyList()
        } catch (e: Exception) {
            logger.severe("Error retrieving servers for group '$groupName': ${e.message}")
            emptyList()
        }
    }

    suspend fun getServerByNumericalId(groupName: String, numericalId: Int): Server? {
        return try {
            plugin.api.server().getAllServers(
                ServerQuery.create()
                    .filterByServerGroupName(resolveGroupNameCasing(groupName))
                    .filterByNumericalId(numericalId)
            ).await()?.firstOrNull()
        } catch (e: Exception) {
            logger.severe("Error retrieving server '$groupName-$numericalId': ${e.message}")
            null
        }
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
            servers.filter { server ->
                val persistentServerName = server.persistentServer?.name ?: return@filter false
                persistentServerName in names
            }
        } catch (e: Exception) {
            logger.severe("Error retrieving persistent servers by name: ${e.message}")
            emptyList()
        }
    }

    suspend fun getServiceProperty(groupName: String, numericalId: Int, key: String): String? {
        val server = getServerByNumericalId(groupName, numericalId) ?: return null
        return server.properties[key]?.toString().orNullIfBlank()
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
        val results = getServersByGroup(groupName).map { updateServerProperty(it.serverId, key, value) }
        val allSuccessful = results.all { it }

        if (allSuccessful) {
            logger.info("Service property '$key' updated to '$value' for all services in group '$groupName'")
        }
        return allSuccessful
    }

    private suspend fun getGroupByName(groupName: String): Group? {
        val group = try {
            plugin.api.group().getGroupByName(groupName).await()
        } catch (_: Exception) {
            null
        }
        if (group != null) {
            return group
        }

        return try {
            plugin.api.group().allGroups.await().firstOrNull { it.name.equals(groupName, ignoreCase = true) }
        } catch (e: Exception) {
            logger.severe("Error retrieving group '$groupName': ${e.message}")
            null
        }
    }

    private suspend fun resolveGroupNameCasing(groupName: String): String =
        getGroupByName(groupName)?.name ?: groupName

    private fun String?.orNullIfBlank(): String? {
        if (isNullOrBlank()) return null
        return this
    }
}
