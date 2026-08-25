package app.simplecloud.plugin.proxy.shared.handler

import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import app.simplecloud.plugin.proxy.shared.config.DomainMotdRoute
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import kotlin.time.Duration.Companion.milliseconds

class DomainMotdHandler(
    private val proxyPlugin: ProxyPlugin
) {
    private val logger = Logger.getLogger(DomainMotdHandler::class.java.name)
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
                    logger.severe("Error while syncing domain MOTD routes: ${e.message}")
                }
                delay(3000.milliseconds)
            }
        }
    }

    fun registerListener() {
        proxyPlugin.api.event().server().onUpdated { event ->
            val server = event.server
            val matchingRoutes = proxyPlugin.proxyEssentialsConfig.get().domains.filter { route ->
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
        val routes = proxyPlugin.proxyEssentialsConfig.get().domains
        val configuredDomains = routes.map { normalize(it.domain) }.toSet()
        resolvedLayouts.keys.retainAll(configuredDomains)

        routes.forEach { refreshRoute(it) }
    }

    private suspend fun refreshRoute(route: DomainMotdRoute) {
        val domainKey = normalize(route.domain)
        val layoutName = resolveRoute(route)

        if (layoutName != null) resolvedLayouts[domainKey] = layoutName
        else resolvedLayouts.remove(domainKey)
    }

    private suspend fun resolveRoute(route: DomainMotdRoute): String? {
        val target = route.target
        val state = if (proxyPlugin.cloudControllerHandler.groupExists(target)) {
            proxyPlugin.joinStateHandler.getJoinStateAtGroup(target)
        } else {
            proxyPlugin.joinStateHandler.getJoinStateAtPersistentServer(target)
        }
        val ruleLayout = route.rules.find { it.state == state }?.layout
            ?.takeIf { hasLayout(it) }

        if (ruleLayout != null) return ruleLayout

        val config = proxyPlugin.proxyEssentialsConfig.get()
        val globalLayout = config.joinstates.find { it.name == state }?.forcedMotdLayout
            ?.takeIf { hasLayout(it) }

        if (globalLayout != null) return globalLayout

        return config.initialLayout.takeIf { hasLayout(it) }
    }

    private fun hasLayout(name: String): Boolean = proxyPlugin.motdLayoutHandler.getLayoutByName(name) != null

    private fun normalize(domain: String): String = domain.trim().lowercase()
}
