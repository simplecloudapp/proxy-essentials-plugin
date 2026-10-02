package app.simplecloud.plugin.proxy.shared.layout

import app.simplecloud.api.CloudApi
import app.simplecloud.api.runtime.SimpleCloudRuntime
import app.simplecloud.plugin.api.shared.config.ConfigurationFactory
import app.simplecloud.plugin.proxy.shared.config.DomainRoute
import app.simplecloud.plugin.proxy.shared.config.LayoutConfig
import app.simplecloud.plugin.proxy.shared.config.MotdEntry
import app.simplecloud.plugin.proxy.shared.config.MotdUpdateType
import app.simplecloud.plugin.proxy.shared.config.ProxyEssentialsConfig
import app.simplecloud.plugin.proxy.shared.joinstate.JoinStateService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class LayoutService(
    private val api: CloudApi,
    private val repository: LayoutRepository,
    private val service: JoinStateService,
    private val config: ConfigurationFactory<ProxyEssentialsConfig>,
    private val scope: CoroutineScope
) {

    private val logger = LoggerFactory.getLogger(LayoutService::class.java)
    private val localLayout = AtomicReference<String?>()
    private val domainLayouts = AtomicReference<Map<String, String>>(emptyMap())
    private val pingCounters = ConcurrentHashMap<String, AtomicInteger>()

    suspend fun start() {
        val server = api.server().getServerById(SimpleCloudRuntime.serverId()).await()
        val groupName = server.group?.name
        val groupLayout = if (groupName == null) null else getGroupLayout(groupName)
        localLayout.set(readLayout(server.properties) ?: groupLayout)

        api.event().server().onUpdated { event ->
            if (event.serverId != server.serverId) return@onUpdated

            val layout = readLayout(event.server.properties)
            if (layout != null && localLayout.getAndSet(layout) != layout) {
                logger.info("Layout changed to '$layout'")
            }
        }
    }

    fun startDomainSync() {
        scope.launch {
            while (isActive) {
                try {
                    updateDomainLayouts()
                } catch (e: Exception) {
                    logger.error("Could not update the layouts of the configured domains", e)
                }
                delay(3.seconds)
            }
        }
    }

    fun getLayout(name: String): LayoutConfig = repository.find(name) ?: LayoutConfig()

    fun getLayoutName(domain: String?): String {
        val config = config.get()
        val domainLayout = if (domain == null) null else domainLayouts.get()[domain.lowercase()]
        val forcedLayout = config.joinstates.find { it.name == service.getLocalState() }?.forcedMotdLayout
        val candidates = listOf(domainLayout, forcedLayout, localLayout.get(), config.initialLayout)

        return candidates.filterNotNull().firstOrNull { repository.find(it) != null } ?: config.initialLayout
    }

    fun getMotdEntry(layoutName: String, layout: LayoutConfig): MotdEntry? {
        val motd = layout.motd
        if (motd.layouts.isEmpty()) return null

        val updateTime = motd.updateTime ?: return motd.layouts[getPingIndex(layoutName, motd.updateType, motd.layouts.size)]
        if (updateTime <= 0) return motd.layouts.first()

        val timeSlot = System.currentTimeMillis() / (updateTime * 50L)
        val index = when (motd.updateType) {
            MotdUpdateType.RANDOM -> Random(timeSlot).nextInt(motd.layouts.size)
            MotdUpdateType.QUEUE -> timeSlot.mod(motd.layouts.size)
        }
        return motd.layouts[index]
    }

    suspend fun getGroupLayout(groupName: String): String? =
        readLayout(api.group().getGroupByName(groupName).await().properties)

    suspend fun setGroupLayout(groupName: String, layout: String) {
        val group = api.group().getGroupByName(groupName).await()
        api.group().updateGroupProperty(group.serverGroupId, KEY, layout).await()
        api.server().getServersByGroup(groupName).await().forEach {
            api.server().updateServerProperty(it.serverId, KEY, layout).await()
        }
    }

    private suspend fun updateDomainLayouts() {
        val resolved = config.get().domains.mapNotNull { route ->
            val layout = getDomainLayout(route) ?: return@mapNotNull null
            route.domain.lowercase() to layout
        }
        domainLayouts.set(resolved.toMap())
    }

    private suspend fun getDomainLayout(route: DomainRoute): String? {
        val config = config.get()
        val state = service.getTargetState(route.target)
        val candidates = listOf(
            route.rules.find { it.state == state }?.layout,
            config.joinstates.find { it.name == state }?.forcedMotdLayout,
            config.initialLayout
        )

        return candidates.filterNotNull().firstOrNull { repository.find(it) != null }
    }

    private fun getPingIndex(layoutName: String, updateType: MotdUpdateType, size: Int): Int {
        return when (updateType) {
            MotdUpdateType.RANDOM -> Random.nextInt(size)
            MotdUpdateType.QUEUE -> pingCounters.computeIfAbsent(layoutName) { AtomicInteger() }.getAndIncrement().mod(size)
        }
    }

    private fun readLayout(properties: Map<String, Any>?): String? {
        val layout = properties?.get(KEY)?.toString()
        if (layout.isNullOrBlank()) return null
        return layout
    }

    companion object {
        const val KEY = "motd-layout"
    }
}
