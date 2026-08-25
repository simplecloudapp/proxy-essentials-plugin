package app.simplecloud.plugin.proxy.shared

import app.simplecloud.api.CloudApi
import app.simplecloud.api.CloudApiOptions
import app.simplecloud.plugin.api.shared.config.ConfigurationFactory
import app.simplecloud.plugin.proxy.shared.config.DefaultConfigInstaller
import app.simplecloud.plugin.proxy.shared.config.MessageConfig
import app.simplecloud.plugin.proxy.shared.config.OldConfigMigrator
import app.simplecloud.plugin.proxy.shared.config.PlaceHolderConfiguration
import app.simplecloud.plugin.proxy.shared.config.ProxyEssentialsConfig
import app.simplecloud.plugin.proxy.shared.handler.CloudControllerHandler
import app.simplecloud.plugin.proxy.shared.handler.DomainMotdHandler
import app.simplecloud.plugin.proxy.shared.handler.JoinStateHandler
import app.simplecloud.plugin.proxy.shared.handler.JoinStateResolver
import app.simplecloud.plugin.proxy.shared.handler.MotdLayoutHandler
import app.simplecloud.plugin.proxy.shared.handler.PlayerCountHandler
import app.simplecloud.plugin.proxy.shared.handler.ProxyJoinGate
import app.simplecloud.plugin.proxy.shared.handler.TabListResolver
import java.io.File

class ProxyPlugin(
    dirPath: String
) {
    val api = CloudApi.create(CloudApiOptions.builder().component("proxy-essentials").build())

    private val dataDirectory = File(dirPath)

    init {
        dataDirectory.mkdirs()
        OldConfigMigrator.migrate(dataDirectory.toPath())
        DefaultConfigInstaller.install(dataDirectory.toPath(), javaClass.classLoader)
    }

    val proxyEssentialsConfig = ConfigurationFactory(File(dataDirectory, "config.yml"), ProxyEssentialsConfig::class.java)
    val messagesConfiguration = ConfigurationFactory(File(dataDirectory, "messages.yml"), MessageConfig::class.java)
    val placeHolderConfiguration = ConfigurationFactory(File(dataDirectory, "placeholder.yml"), PlaceHolderConfiguration::class.java)

    init {
        loadConfigurations()
    }

    val serverIconsPath = "$dirPath/layout/server-icons"
    val motdLayoutHandler = MotdLayoutHandler(File("$dirPath/layout").toPath(), this)
    val joinStateHandler = JoinStateHandler(this)
    val cloudControllerHandler = CloudControllerHandler(this, joinStateHandler)
    val playerCountHandler = PlayerCountHandler(this).also { it.start() }
    val joinStateResolver = JoinStateResolver(this)
    val proxyJoinGate = ProxyJoinGate(
        localState = { joinStateHandler.localState },
        resolveJoinState = joinStateResolver::resolveJoinState,
        isServerFull = joinStateResolver::isServerFull,
        kickMessages = { messagesConfiguration.get().kick }
    )
    val tabListResolver = TabListResolver { proxyEssentialsConfig.get().tablist }
    val domainMotdHandler = DomainMotdHandler(this).also {
        it.registerListener()
        it.startSyncTask()
    }

    fun reload() {
        loadConfigurations()
        motdLayoutHandler.loadMotdLayouts()
    }

    fun shutdown() {
        playerCountHandler.stop()
        joinStateHandler.stop()
        cloudControllerHandler.close()
        domainMotdHandler.stop()
    }

    private fun loadConfigurations() {
        proxyEssentialsConfig.loadOrCreate(ProxyEssentialsConfig())
        messagesConfiguration.loadOrCreate(MessageConfig())
        placeHolderConfiguration.loadOrCreate(PlaceHolderConfiguration())
    }
}
