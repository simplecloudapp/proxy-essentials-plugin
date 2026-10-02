package app.simplecloud.plugin.proxy.velocity

import app.simplecloud.plugin.proxy.shared.ProxyEssentials
import app.simplecloud.plugin.proxy.shared.command.ProxyCommandHandler
import app.simplecloud.plugin.proxy.velocity.command.VelocityCommandSender
import app.simplecloud.plugin.proxy.velocity.listener.LoginListener
import app.simplecloud.plugin.proxy.velocity.listener.ProxyPingListener
import app.simplecloud.plugin.proxy.velocity.listener.ServerKickListener
import app.simplecloud.plugin.proxy.velocity.listener.ServerPreConnectListener
import app.simplecloud.plugin.proxy.velocity.listener.TabListListener
import app.simplecloud.plugin.proxy.velocity.platform.VelocityPlatformImpl
import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.velocity.VelocityCommandManager
import java.nio.file.Path

@Plugin(
    id = "simplecloud-proxy-essentials",
    name = "simplecloud-proxy-essentials",
    version = BuildConstants.VERSION,
    authors = ["D151l"],
    description = "Configure SimpleCloud MOTDs, tablists, join states, player counts, and proxy layouts",
    url = "https://github.com/simplecloudapp/proxy-essentials-plugin",
    dependencies = [
        Dependency("simplecloud-api")
    ]
)
class ProxyVelocityPlugin @Inject constructor(
    private val server: ProxyServer,
    @DataDirectory dataDirectory: Path
) {

    private val essentials = ProxyEssentials(VelocityPlatformImpl(server, dataDirectory))
    private val tabListHandler = TabListHandler(this, server, essentials)

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        essentials.start()

        server.eventManager.register(this, LoginListener(essentials))
        server.eventManager.register(this, ServerPreConnectListener(essentials))
        server.eventManager.register(this, ProxyPingListener(essentials))
        server.eventManager.register(this, ServerKickListener(essentials))
        server.eventManager.register(this, TabListListener(tabListHandler))

        ProxyCommandHandler(createCommandManager(), essentials).register()
        tabListHandler.start()
    }

    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        essentials.stop()
    }

    private fun createCommandManager(): VelocityCommandManager<VelocityCommandSender> {
        return VelocityCommandManager(
            server.pluginManager.ensurePluginContainer(this),
            server,
            ExecutionCoordinator.asyncCoordinator(),
            SenderMapper.create(::VelocityCommandSender, VelocityCommandSender::source)
        )
    }
}
