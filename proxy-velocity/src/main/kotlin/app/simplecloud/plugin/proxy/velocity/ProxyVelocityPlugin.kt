package app.simplecloud.plugin.proxy.velocity

import app.simplecloud.plugin.proxy.shared.config.YamlConfig
import app.simplecloud.plugin.proxy.shared.config.motd.MotdConfiguration
import app.simplecloud.plugin.proxy.shared.config.tablis.TabListConfiguration
import app.simplecloud.plugin.proxy.velocity.handler.TabListHandler
import app.simplecloud.plugin.proxy.velocity.listener.ProxyPingConfigurationListener
import app.simplecloud.plugin.proxy.velocity.listener.ProxyPingListener
import app.simplecloud.plugin.proxy.velocity.listener.TabListListener
import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import java.nio.file.Path
import kotlin.io.path.pathString

class ProxyVelocityPlugin @Inject constructor(
    val proxyServer: ProxyServer,
    @DataDirectory val dataDirectory: Path
) {

    val tabListHandler = TabListHandler(this)

    lateinit var motdConfiguration: MotdConfiguration
    lateinit var tabListConfiguration: TabListConfiguration

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        val config = YamlConfig(this.dataDirectory.pathString)

        this.motdConfiguration = config.load<MotdConfiguration>("motd-configuration")!!
        config.save("motd-configuration", this.motdConfiguration)

        this.tabListConfiguration = config.load<TabListConfiguration>("tablist-configuration")!!
        config.save("tablist-configuration", this.tabListConfiguration)

        this.proxyServer.eventManager.register(this, ProxyPingListener(this))
        this.proxyServer.eventManager.register(this, ProxyPingConfigurationListener(this))
        this.proxyServer.eventManager.register(this, TabListListener(this))

        this.tabListHandler.startTabListTask()
    }
}