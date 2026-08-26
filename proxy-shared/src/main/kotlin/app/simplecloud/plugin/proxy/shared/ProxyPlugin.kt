package app.simplecloud.plugin.proxy.shared

import app.simplecloud.api.CloudApi
import app.simplecloud.api.CloudApiOptions
import app.simplecloud.plugin.api.shared.config.ConfigurationFactory
import app.simplecloud.plugin.proxy.shared.config.DefaultConfigInstaller
import app.simplecloud.plugin.proxy.shared.config.MessageConfig
import app.simplecloud.plugin.proxy.shared.config.PlaceHolderConfiguration
import app.simplecloud.plugin.proxy.shared.config.ProxyEssentialsConfig
import app.simplecloud.plugin.proxy.shared.config.migration.OldConfigMigrator
import app.simplecloud.plugin.proxy.shared.controller.CloudControllerHandler
import app.simplecloud.plugin.proxy.shared.joinstate.JoinStateResolver
import app.simplecloud.plugin.proxy.shared.joinstate.JoinStateService
import app.simplecloud.plugin.proxy.shared.joinstate.ProxyJoinGate
import app.simplecloud.plugin.proxy.shared.motd.DomainMotdResolver
import app.simplecloud.plugin.proxy.shared.motd.MotdLayoutRepository
import app.simplecloud.plugin.proxy.shared.player.PlayerCountTracker
import app.simplecloud.plugin.proxy.shared.tablist.TabListResolver
import java.io.File

class ProxyPlugin(
    dirPath: String
) {

    val api: CloudApi = CloudApi.create(CloudApiOptions.builder().component("proxy-essentials").build())

    private val dataDirectory = File(dirPath)

    val config = ConfigurationFactory(File(dataDirectory, "config.yml"), ProxyEssentialsConfig::class.java)
    val messageConfig = ConfigurationFactory(File(dataDirectory, "messages.yml"), MessageConfig::class.java)
    val placeholderConfig = ConfigurationFactory(File(dataDirectory, "placeholder.yml"), PlaceHolderConfiguration::class.java)

    init {
        dataDirectory.mkdirs()
        OldConfigMigrator.migrate(dataDirectory.toPath())
        DefaultConfigInstaller.install(dataDirectory.toPath(), javaClass.classLoader)
        loadConfigurations()
    }

    val serverIconsPath = "$dirPath/layout/server-icons"
    val layoutRepository = MotdLayoutRepository(File("$dirPath/layout").toPath(), this)
    val joinStateService = JoinStateService(this)
    val cloudControllerHandler = CloudControllerHandler(this, joinStateService)
    val playerCountTracker = PlayerCountTracker(this)
    val joinStateResolver = JoinStateResolver(this)
    val proxyJoinGate = ProxyJoinGate(this)
    val tabListResolver = TabListResolver(this)
    val domainMotdHandler = DomainMotdResolver(this)

    fun start() {
        layoutRepository.loadMotdLayouts()
        cloudControllerHandler.start()
        playerCountTracker.start()
        domainMotdHandler.registerListener()
        domainMotdHandler.startSyncTask()
    }

    fun shutdown() {
        playerCountTracker.stop()
        joinStateService.stop()
        cloudControllerHandler.close()
        domainMotdHandler.stop()
    }

    fun reload() {
        loadConfigurations()
        layoutRepository.loadMotdLayouts()
    }

    private fun loadConfigurations() {
        config.loadOrCreate(ProxyEssentialsConfig())
        messageConfig.loadOrCreate(MessageConfig())
        placeholderConfig.loadOrCreate(PlaceHolderConfiguration())
    }
}
