package app.simplecloud.plugin.proxy.velocity

import app.simplecloud.plugin.api.shared.extension.miniMessage
import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import app.simplecloud.plugin.proxy.shared.command.ProxyEssentialsCommandHandler
import app.simplecloud.plugin.proxy.shared.command.commands.JoinStateCommandHandler
import app.simplecloud.plugin.proxy.shared.command.commands.LayoutCommandHandler
import app.simplecloud.plugin.proxy.shared.utilities.format.MotdMiniMessageFormatter
import app.simplecloud.plugin.proxy.velocity.listener.LoginListener
import app.simplecloud.plugin.proxy.velocity.listener.ServerKickListener
import app.simplecloud.plugin.proxy.velocity.listener.ServerPreConnectListener
import app.simplecloud.plugin.proxy.velocity.listener.ProxyPingListener
import app.simplecloud.plugin.proxy.velocity.placeholder.ConfigureTagResolversEvent
import app.simplecloud.plugin.proxy.velocity.placeholder.ConfigureTagResolversListener
import app.simplecloud.plugin.proxy.velocity.tablist.TabListHandler
import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.Component
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.velocity.VelocityCommandManager
import org.slf4j.Logger
import java.nio.file.Path
import kotlin.io.path.pathString

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
    val server: ProxyServer,
    @DataDirectory val dataDirectory: Path,
    val logger: Logger,
) {

    val plugin = ProxyPlugin(dataDirectory.pathString)
    val tabListHandler = TabListHandler(plugin, this, server)

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        plugin.start()

        registerListeners()
        registerCommands()
        startTabListTask()
    }

    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        tabListHandler.stopTabListTask()
        plugin.shutdown()
    }

    fun deserializeToComponent(text: String, player: Player? = null): Component {
        val tagResolvers = fireConfigureTagResolvers(player)
        return miniMessage.deserialize(text, *tagResolvers.toTypedArray())
    }

    fun deserializeMotd(line1: String, line2: String): Component {
        val tagResolvers = fireConfigureTagResolvers(null)
        return MotdMiniMessageFormatter.deserialize(miniMessage, line1, line2, tagResolvers)
    }

    private fun registerListeners() {
        val manager = server.eventManager

        manager.register(this, ProxyPingListener(plugin, this))
        manager.register(this, ConfigureTagResolversListener(plugin, this))
        manager.register(this, LoginListener(plugin, this))
        manager.register(this, ServerPreConnectListener(plugin, this))
        manager.register(this, ServerKickListener(plugin))
    }

    private fun registerCommands() {
        val manager = VelocityCommandManager(
            server.pluginManager.ensurePluginContainer(this),
            server,
            ExecutionCoordinator.asyncCoordinator(),
            SenderMapper.create(
                { commandSource -> VelocityCommandSender(commandSource, this) },
                { cloudSender -> cloudSender.commandSource }
            )
        )

        ProxyEssentialsCommandHandler(manager, plugin).loadCommands()
        JoinStateCommandHandler(manager, plugin).loadCommands()
        LayoutCommandHandler(manager, plugin).loadCommands()
    }

    private fun startTabListTask() {
        if (plugin.config.get().tabListUpdateTimeMillis() <= 0) {
            logger.info("Tablist update time is set to 0, tablist will not be updated automatically")
            return
        }

        tabListHandler.startTabListTask()
    }

    private fun fireConfigureTagResolvers(player: Player?) = server.eventManager.fire(ConfigureTagResolversEvent(player)).get().tagResolvers
}
