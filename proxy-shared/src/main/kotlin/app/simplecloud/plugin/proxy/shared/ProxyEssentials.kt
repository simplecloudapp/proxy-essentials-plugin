package app.simplecloud.plugin.proxy.shared

import app.simplecloud.api.CloudApi
import app.simplecloud.api.CloudApiOptions
import app.simplecloud.api.runtime.SimpleCloudRuntime
import app.simplecloud.plugin.api.shared.config.ConfigurationFactory
import app.simplecloud.plugin.api.shared.config.DefaultConfigInstaller
import app.simplecloud.plugin.proxy.shared.config.MessageConfig
import app.simplecloud.plugin.proxy.shared.config.ProxyEssentialsConfig
import app.simplecloud.plugin.proxy.shared.config.migration.LegacyDirectoryMigrator
import app.simplecloud.plugin.proxy.shared.joinstate.JoinGate
import app.simplecloud.plugin.proxy.shared.joinstate.JoinStateService
import app.simplecloud.plugin.proxy.shared.layout.LayoutRepository
import app.simplecloud.plugin.proxy.shared.layout.LayoutService
import app.simplecloud.plugin.proxy.shared.platform.ProxyPlatform
import app.simplecloud.plugin.proxy.shared.player.PlayerCountService
import app.simplecloud.plugin.proxy.shared.tablist.TabListService
import app.simplecloud.plugin.proxy.shared.utilities.text.MessageFormatter
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.time.Duration.Companion.seconds

class ProxyEssentials(
    platform: ProxyPlatform
) {

    private val logger = LoggerFactory.getLogger(ProxyEssentials::class.java)
    private val dir = platform.getDataDirectory()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val api: CloudApi = CloudApi.create(CloudApiOptions.builder().component("proxy-essentials").build())

    val config = ConfigurationFactory(dir.resolve("config.yml").toFile(), ProxyEssentialsConfig::class.java)
    val messages = ConfigurationFactory(dir.resolve("messages.yml").toFile(), MessageConfig::class.java)

    val joinStateService = JoinStateService(api, config, scope)
    val layoutRepository = LayoutRepository(dir.resolve("layout"))
    val layoutService = LayoutService(api, layoutRepository, joinStateService, config, scope)
    val playerCountService = PlayerCountService(api, platform, config, scope)
    val joinGate = JoinGate(api, joinStateService, config, messages, scope)
    val messageFormatter = MessageFormatter(messages, playerCountService, layoutService)
    val tabListService = TabListService(platform, config, messageFormatter, scope)
    val serverIconDirectory: Path = dir.resolve("layout").resolve("server-icons")

    fun start() {
        LegacyDirectoryMigrator.migrate(dir)
        DefaultConfigInstaller.installAll(
            dir,
            listOf(
                "config.yml",
                "messages.yml",
                "layout/public.yml",
                "layout/maintenance.yml",
                "layout/server-icons/simplecloud.png",
                "layout/server-icons/simplecloud-gray.png"
            ),
            javaClass.classLoader,
            "config"
        )
        loadConfigs()
        layoutService.startDomainSync()
        tabListService.start()

        if (SimpleCloudRuntime.serverId().isBlank()) return
        joinStateService.startServerStateSync()
        joinGate.start()
        playerCountService.start()
        startJoinStateService()
        startLayoutService()
    }

    fun stop() {
        scope.cancel()
        api.close()
    }

    fun reload() {
        loadConfigs()
    }

    private fun startJoinStateService() {
        scope.launch {
            while (isActive) {
                try {
                    joinStateService.start()
                    return@launch
                } catch (e: Exception) {
                    logger.error("Failed to load the join state of this server from the controller, using the initial join state and retrying in 5 seconds", e)
                }
                delay(5.seconds)
            }
        }
    }

    private fun startLayoutService() {
        scope.launch {
            while (isActive) {
                try {
                    layoutService.start()
                    return@launch
                } catch (e: Exception) {
                    logger.error("Failed to load the layout of this server from the controller, using the initial layout and retrying in 5 seconds", e)
                }
                delay(5.seconds)
            }
        }
    }

    private fun loadConfigs() {
        config.loadOrCreate(ProxyEssentialsConfig())
        messages.loadOrCreate(MessageConfig())
        layoutRepository.loadLayouts()
    }
}
