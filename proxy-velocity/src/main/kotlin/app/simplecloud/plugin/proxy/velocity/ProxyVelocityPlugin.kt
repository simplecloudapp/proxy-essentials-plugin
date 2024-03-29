package app.simplecloud.plugin.proxy.velocity

import app.simplecloud.plugin.proxy.shared.config.YamlConfig
import app.simplecloud.plugin.proxy.shared.config.motd.MotdConfiguration
import app.simplecloud.plugin.proxy.shared.config.placeholder.PlaceHolderConfiguration
import app.simplecloud.plugin.proxy.shared.config.tablis.TabListConfiguration
import app.simplecloud.plugin.proxy.velocity.handler.TabListHandler
import app.simplecloud.plugin.proxy.velocity.listener.MotdConfigurationListener
import app.simplecloud.plugin.proxy.velocity.listener.ProxyPingListener
import app.simplecloud.plugin.proxy.velocity.listener.TabListListener
import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import org.slf4j.Logger
import java.nio.file.Path
import kotlin.io.path.pathString

class ProxyVelocityPlugin @Inject constructor(
    val proxyServer: ProxyServer,
    @DataDirectory val dataDirectory: Path,
    val logger: Logger
) {

    val tabListHandler = TabListHandler(this)

    lateinit var motdConfiguration: MotdConfiguration
    lateinit var tabListConfiguration: TabListConfiguration
    lateinit var placeHolderConfiguration: PlaceHolderConfiguration

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        val config = YamlConfig(this.dataDirectory.pathString)

        this.motdConfiguration = config.load<MotdConfiguration>("motd")!!
        config.save("motd", this.motdConfiguration)

        this.tabListConfiguration = config.load<TabListConfiguration>("tablist")!!
        config.save("tablist", this.tabListConfiguration)

        this.placeHolderConfiguration = config.load<PlaceHolderConfiguration>("placeholder")!!
        config.save("placeholder", this.placeHolderConfiguration)

        this.proxyServer.eventManager.register(this, ProxyPingListener(this))
        this.proxyServer.eventManager.register(this, MotdConfigurationListener(this))
        this.proxyServer.eventManager.register(this, TabListListener(this))
        if (this.tabListConfiguration.tabListUpdateTime > 0)
            this.tabListHandler.startTabListTask()
        else
            this.logger.info("Tablist update time is set to 0, tablist will not be updated automatically")
    }

    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        this.tabListHandler.stopTabListTask()
    }
}