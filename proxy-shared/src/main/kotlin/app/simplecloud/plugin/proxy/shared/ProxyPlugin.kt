package app.simplecloud.plugin.proxy.shared

import app.simplecloud.api.CloudApi
import app.simplecloud.api.CloudApiOptions
import app.simplecloud.plugin.proxy.shared.config.DefaultConfigInstaller
import app.simplecloud.plugin.proxy.shared.config.OldConfigMigrator
import app.simplecloud.plugin.proxy.shared.config.ProxyEssentialsConfig
import app.simplecloud.plugin.proxy.shared.config.YamlConfig
import app.simplecloud.plugin.proxy.shared.config.message.MessageConfig
import app.simplecloud.plugin.proxy.shared.config.placeholder.PlaceHolderConfiguration
import app.simplecloud.plugin.proxy.shared.handler.CloudControllerHandler
import app.simplecloud.plugin.proxy.shared.handler.DomainMotdHandler
import app.simplecloud.plugin.proxy.shared.handler.JoinStateHandler
import app.simplecloud.plugin.proxy.shared.handler.JoinStateResolver
import app.simplecloud.plugin.proxy.shared.handler.MotdLayoutHandler
import app.simplecloud.plugin.proxy.shared.handler.PlayerCountHandler
import app.simplecloud.plugin.proxy.shared.handler.TabListResolver
import java.io.File
import java.nio.file.Path

class ProxyPlugin(
    dirPath: String
) {
    val api = CloudApi.create(CloudApiOptions.builder().component("proxy-essentials").build())

    init {
        val dataDirectory = Path.of(dirPath)
        OldConfigMigrator.migrate(dataDirectory)
        DefaultConfigInstaller.install(dataDirectory, javaClass.classLoader)
    }

    val config = YamlConfig(dirPath)
    val proxyEssentialsConfig = config.load<ProxyEssentialsConfig>("config")
    val placeHolderConfiguration = config.load<PlaceHolderConfiguration>("placeholder")
    val messagesConfiguration = config.load<MessageConfig>("messages")

    val serverIconsPath = "$dirPath/layout/server-icons"
    val motdLayoutHandler = MotdLayoutHandler(File("$dirPath/layout").toPath(), this)
    val joinStateHandler = JoinStateHandler(this)
    val cloudControllerHandler = CloudControllerHandler(this, joinStateHandler)
    val playerCountHandler = PlayerCountHandler(this).also { it.start() }
    val joinStateResolver = JoinStateResolver(this)
    val tabListResolver = TabListResolver { proxyEssentialsConfig.get().tablist }
    val domainMotdHandler = DomainMotdHandler(this).also {
        it.registerListener()
        it.startSyncTask()
    }

    fun shutdown() {
        playerCountHandler.stop()
        joinStateHandler.stop()
        cloudControllerHandler.close()
        motdLayoutHandler.close()
        domainMotdHandler.stop()
        config.close()
    }
}
