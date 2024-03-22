package app.simplecloud.plugin.proxy.velocity

import app.simplecloud.plugin.proxy.shared.config.YamlConfig
import app.simplecloud.plugin.proxy.shared.config.motd.MotdConfiguration
import app.simplecloud.plugin.proxy.velocity.listener.ProxyPingConfigurationListener
import app.simplecloud.plugin.proxy.velocity.listener.ProxyPingListener
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

    lateinit var motdConfiguration: MotdConfiguration
    lateinit var maintenanceMotdConfiguration: MotdConfiguration

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        this.motdConfiguration = YamlConfig(this.dataDirectory.pathString).load<MotdConfiguration>("motd-configuration")!!
        this.maintenanceMotdConfiguration = YamlConfig(this.dataDirectory.pathString).load<MotdConfiguration>("maintenance-motd-configuration")!!

        this.proxyServer.eventManager.register(this, ProxyPingListener(this))
        this.proxyServer.eventManager.register(this, ProxyPingConfigurationListener(this))
    }
}