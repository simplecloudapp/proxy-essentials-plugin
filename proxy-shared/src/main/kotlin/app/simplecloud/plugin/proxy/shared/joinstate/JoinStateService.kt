package app.simplecloud.plugin.proxy.shared.joinstate

import app.simplecloud.api.CloudApi
import app.simplecloud.api.runtime.SimpleCloudRuntime
import app.simplecloud.api.server.Server
import app.simplecloud.plugin.api.shared.config.ConfigurationFactory
import app.simplecloud.plugin.proxy.shared.config.ProxyEssentialsConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.seconds

class JoinStateService(
    private val api: CloudApi,
    private val config: ConfigurationFactory<ProxyEssentialsConfig>,
    private val scope: CoroutineScope
) {

    private val logger = LoggerFactory.getLogger(JoinStateService::class.java)
    private val localState = AtomicReference<String?>()
    private val lastGroupState = AtomicReference<String?>()
    private val serverStates = AtomicReference<Map<String, String>?>()

    fun getLocalState(): String = localState.get() ?: config.get().initialState

    suspend fun start() {
        val server = api.server().getServerById(SimpleCloudRuntime.serverId()).await()
        localState.set(readState(server.properties) ?: applyDefaultState(server))

        api.event().server().onUpdated { event ->
            if (event.serverId != server.serverId) return@onUpdated
            scope.launch { updateLocalState(event.server) }
        }

        val groupName = server.group?.name ?: return
        scope.launch {
            while (isActive) {
                try {
                    followGroupState(server.serverId, groupName)
                } catch (e: Exception) {
                    logger.error("Could not synchronize the join state with group '$groupName'", e)
                }
                delay(2.seconds)
            }
        }
    }

    suspend fun getGroupState(groupName: String): String =
        readState(api.group().getGroupByName(groupName).await().properties) ?: config.get().initialState

    suspend fun getServiceState(groupName: String, numericalId: Int): String {
        val service = api.server().getServerByNumericalId(groupName, numericalId).await()
        return readState(service.properties) ?: getGroupState(groupName)
    }

    suspend fun getPersistentServerState(name: String): String =
        readState(api.persistentServer().getPersistentServerByName(name).await().properties) ?: config.get().initialState

    suspend fun getTargetState(name: String): String {
        if (isGroup(name)) return getGroupState(name)
        return getPersistentServerState(name)
    }

    fun startServerStateSync() {
        scope.launch {
            while (isActive) {
                try {
                    refreshServerStates()
                } catch (e: Exception) {
                    logger.error("Could not load the join states of the servers, keeping the last known states", e)
                }
                delay(2.seconds)
            }
        }
    }

    fun getServerState(serverName: String): String? {
        val states = serverStates.get() ?: return null
        return states[serverName] ?: config.get().initialState
    }

    suspend fun setGroupState(groupName: String, state: String) {
        val group = api.group().getGroupByName(groupName).await()
        api.group().updateGroupProperty(group.serverGroupId, KEY, state).await()
        api.server().getServersByGroup(groupName).await().forEach { setServerState(it.serverId, state) }
    }

    suspend fun setServiceState(groupName: String, numericalId: Int, state: String) {
        val service = api.server().getServerByNumericalId(groupName, numericalId).await()
        setServerState(service.serverId, state)
    }

    suspend fun setPersistentServerState(name: String, state: String) {
        val persistentServer = api.persistentServer().getPersistentServerByName(name).await()
        api.persistentServer().updatePersistentServerProperty(persistentServer.persistentServerId, KEY, state).await()
        api.server().allServers.await()
            .filter { it.persistentServerId == persistentServer.persistentServerId }
            .forEach { setServerState(it.serverId, state) }
    }

    suspend fun isGroup(name: String): Boolean = api.group().allGroups.await().any { it.name == name }

    private suspend fun setServerState(serverId: String, state: String) {
        api.server().updateServerProperty(serverId, KEY, state).await()
    }

    private suspend fun updateLocalState(server: Server) {
        val state = try {
            readState(server.properties) ?: applyDefaultState(server)
        } catch (e: Exception) {
            logger.error("Could not apply the default join state to this server", e)
            return
        }

        if (localState.getAndSet(state) != state) {
            logger.info("Join state changed to '$state'")
        }
    }

    private suspend fun applyDefaultState(server: Server): String {
        val groupName = server.group?.name
        val persistentServerName = server.persistentServer?.name
        val state = when {
            groupName != null -> getGroupState(groupName)
            persistentServerName != null -> getPersistentServerState(persistentServerName)
            else -> config.get().initialState
        }

        setServerState(server.serverId, state)
        return state
    }

    private suspend fun followGroupState(serverId: String, groupName: String) {
        val groupState = readState(api.group().getGroupByName(groupName).await().properties) ?: return
        val previousGroupState = lastGroupState.getAndSet(groupState)
        if (previousGroupState == null || previousGroupState == groupState) return
        if (getLocalState() != previousGroupState) return

        setServerState(serverId, groupState)
    }

    private suspend fun refreshServerStates() {
        val groupStates = api.group().allGroups.await().associate { it.name to readState(it.properties) }
        val persistentServerStates = api.persistentServer().allPersistentServers.await().associate { it.name to readState(it.properties) }
        val states = api.server().allServers.await().associate { getServerName(it) to resolveState(it, groupStates, persistentServerStates) }
        serverStates.set(states)
    }

    private fun resolveState(server: Server, groupStates: Map<String, String?>, persistentServerStates: Map<String, String?>): String {
        val state = readState(server.properties)
        if (state != null) return state

        val groupName = server.group?.name
        if (groupName != null) return groupStates[groupName] ?: config.get().initialState

        val persistentServerName = server.persistentServer?.name ?: return config.get().initialState
        return persistentServerStates[persistentServerName] ?: config.get().initialState
    }

    private fun readState(properties: Map<String, Any>?): String? {
        val state = properties?.get(KEY)?.toString()
        if (state.isNullOrBlank()) return null
        return state
    }

    private fun getServerName(server: Server): String {
        val groupName = server.group?.name
        if (groupName != null) return "$groupName-${server.numericalId}"
        return server.persistentServer?.name ?: server.serverId
    }

    companion object {
        const val KEY = "joinstate"
    }
}
