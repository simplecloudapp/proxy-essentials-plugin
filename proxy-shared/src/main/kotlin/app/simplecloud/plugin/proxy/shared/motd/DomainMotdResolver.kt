package app.simplecloud.plugin.proxy.shared.motd

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import app.simplecloud.plugin.proxy.shared.config.DomainMotdRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

class DomainMotdResolver(
    private val plugin: ProxyPlugin
) {

    private val logger = LoggerFactory.getLogger(DomainMotdResolver::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var syncJob: Job? = null

    private val resolvedLayouts = ConcurrentHashMap<String, String>()

    fun getLayoutNameForDomain(virtualHost: String): String? = resolvedLayouts[normalize(virtualHost)]

    fun startSyncTask() {
        if (syncJob?.isActive == true) return

        syncJob = scope.launch {
            while (isActive) {
                try {
                    syncAllRoutes()
                } catch (e: Exception) {
                    logger.error("Error while syncing domain MOTD routes", e)
                }
                delay(3000.milliseconds)
            }
        }
    }

    fun registerListener() {
        plugin.api.event().server().onUpdated { event ->
            val server = event.server
            val matchingRoutes = plugin.config.get().domains.filter { route ->
                route.target.equals(server.group?.name, ignoreCase = true) ||
                    route.target.equals(server.persistentServer?.name, ignoreCase = true)
            }

            if (matchingRoutes.isEmpty()) return@onUpdated

            scope.launch {
                matchingRoutes.forEach { refreshRoute(it) }
            }
        }
    }

    fun stop() {
        syncJob?.cancel()
        scope.cancel()
    }

    private suspend fun syncAllRoutes() {
        val routes = plugin.config.get().domains
        resolvedLayouts.keys.retainAll(routes.map { normalize(it.domain) }.toSet())

        routes.forEach { refreshRoute(it) }
    }

    private suspend fun refreshRoute(route: DomainMotdRoute) {
        val domainKey = normalize(route.domain)
        when (val layoutName = resolveRoute(route)) {
            null -> resolvedLayouts.remove(domainKey)
            else -> resolvedLayouts[domainKey] = layoutName
        }
    }

    private suspend fun resolveRoute(route: DomainMotdRoute): String? {
        val config = plugin.config.get()
        val state = getJoinStateOfTarget(route.target)

        val ruleLayout = getExistingLayout(route.rules.find { it.state == state }?.layout)
        if (ruleLayout != null) return ruleLayout

        val forcedLayout = getExistingLayout(config.joinstates.find { it.name == state }?.forcedMotdLayout)
        if (forcedLayout != null) return forcedLayout

        return getExistingLayout(config.initialLayout)
    }

    private suspend fun getJoinStateOfTarget(target: String): String {
        return when {
            plugin.cloudControllerHandler.groupExists(target) -> plugin.joinStateService.getJoinStateAtGroup(target)
            else -> plugin.joinStateService.getJoinStateAtPersistentServer(target)
        }
    }

    private fun getExistingLayout(name: String?): String? {
        if (name == null || plugin.layoutRepository.getLayoutByName(name) == null) {
            return null
        }
        return name
    }

    private fun normalize(domain: String): String = domain.trim().lowercase()
}
