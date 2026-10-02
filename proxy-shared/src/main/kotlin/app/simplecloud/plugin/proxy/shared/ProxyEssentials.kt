package app.simplecloud.plugin.proxy.shared

import app.simplecloud.api.CloudApi
import app.simplecloud.api.CloudApiOptions
import app.simplecloud.api.runtime.SimpleCloudRuntime
import app.simplecloud.plugin.api.shared.config.ConfigurationFactory
import app.simplecloud.plugin.proxy.shared.command.JoinStateCommand
import app.simplecloud.plugin.proxy.shared.command.LayoutCommand
import app.simplecloud.plugin.proxy.shared.command.ProxySender
import app.simplecloud.plugin.proxy.shared.command.ProxyCommand
import app.simplecloud.plugin.proxy.shared.config.MessageConfig
import app.simplecloud.plugin.proxy.shared.config.ProxyEssentialsConfig
import app.simplecloud.plugin.proxy.shared.config.DefaultConfigInstaller
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
import org.incendo.cloud.CommandManager
import org.slf4j.LoggerFactory
import java.nio.file.Path

class ProxyEssentials(
    platform: ProxyPlatform
) {

    private val logger = LoggerFactory.getLogger(ProxyEssentials::class.java)
    private val dir = platform.getDataDirectory()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val api = CloudApi.create(CloudApiOptions.builder().component("proxy-essentials").build())

    val config = ConfigurationFactory(dir.resolve("config.yml").toFile(), ProxyEssentialsConfig::class.java)
    val messages = ConfigurationFactory(dir.resolve("messages.yml").toFile(), MessageConfig::class.java)

    val joinStateService = JoinStateService(api, config, scope)
    val layoutRepository = LayoutRepository(dir.resolve("layout"))
    val layoutService = LayoutService(api, layoutRepository, joinStateService, config, scope)
    val playerCountService = PlayerCountService(api, platform, config, scope)
    val tabListService = TabListService(config)
    val joinGate = JoinGate(api, joinStateService, config, messages)
    val messageFormatter = MessageFormatter(messages, playerCountService, layoutService)
    val serverIconDirectory: Path = dir.resolve("layout").resolve("server-icons")

    fun start() {
        LegacyDirectoryMigrator.migrate(dir)
        DefaultConfigInstaller.install(dir, javaClass.classLoader)
        loadConfigs()
        layoutService.startDomainSync()

        if (SimpleCloudRuntime.serverId().isBlank()) return
        playerCountService.start()
        scope.launch {
            try {
                joinStateService.start()
                layoutService.start()
            } catch (e: Exception) {
                logger.error("Could not load this proxy from the cloud, so it uses the initial join state and layout", e)
            }
        }
    }

    fun stop() {
        scope.cancel()
    }

    fun reload() {
        loadConfigs()
    }

    fun <C : ProxySender> registerCommands(manager: CommandManager<C>) {
        ProxyCommand(manager, this, scope).register()
        JoinStateCommand(manager, api, joinStateService, config, messages, messageFormatter, scope).register()
        LayoutCommand(manager, api, layoutService, layoutRepository, joinStateService, config, messages, messageFormatter, scope).register()
    }

    private fun loadConfigs() {
        config.loadOrCreate(ProxyEssentialsConfig())
        messages.loadOrCreate(MessageConfig())
        layoutRepository.loadLayouts()
    }
}
