package app.simplecloud.plugin.proxy.bungeecord

import app.simplecloud.plugin.proxy.bungeecord.event.ConfigureTagResolversEvent
import app.simplecloud.plugin.proxy.bungeecord.handler.TabListHandler
import app.simplecloud.plugin.proxy.bungeecord.listener.*
import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import app.simplecloud.plugin.proxy.shared.handler.command.CommandSender
import app.simplecloud.plugin.proxy.shared.handler.command.JoinStateCommandHandler
import app.simplecloud.plugin.proxy.shared.handler.command.LayoutCommandHandler
import app.simplecloud.plugin.proxy.shared.handler.command.ProxyEssentialsCommandHandler
import net.kyori.adventure.platform.bungeecord.BungeeAudiences
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Plugin
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.bungee.BungeeCommandManager
import org.incendo.cloud.execution.ExecutionCoordinator


class ProxyBungeeCordPlugin: Plugin() {

    val proxyPlugin = ProxyPlugin(this.dataFolder.path)

    val tabListHandler = TabListHandler(this)

    private var adventure: BungeeAudiences? = null

    private val miniMessage = MiniMessage.miniMessage()

    override fun onEnable() {
        this.proxyPlugin.motdLayoutHandler.loadMotdLayouts()

        this.adventure = BungeeAudiences.create(this)
        this.proxy.pluginManager.registerListener(this, ProxyPingListener(this))
        this.proxy.pluginManager.registerListener(this, ConfigureTagResolversListener(this))
        this.proxy.pluginManager.registerListener(this, ServerPreConnectListener(this))
        this.proxy.pluginManager.registerListener(this, TabListListener(this))

        if (this.proxyPlugin.proxyEssentialsConfig.get().tabListUpdateTimeMillis() > 0)
            this.tabListHandler.startTabListTask()
        else
            this.logger.info("Tablist update time is set to 0, tablist will not be updated automatically")

        val executionCoordinator = ExecutionCoordinator.simpleCoordinator<CommandSender>()

        val senderMapper = SenderMapper.create<net.md_5.bungee.api.CommandSender, CommandSender>(
            { commandSender -> BungeeCordCommandSender(commandSender, adventure) },
            { cloudSender -> (cloudSender as BungeeCordCommandSender).getCommandSender() }
        )

        val commandManager = BungeeCommandManager(
            this,
            executionCoordinator,
            senderMapper
        )

        ProxyEssentialsCommandHandler(commandManager, this.proxyPlugin).loadCommands()
        JoinStateCommandHandler(commandManager, this.proxyPlugin).loadCommands()
        LayoutCommandHandler(commandManager, this.proxyPlugin).loadCommands()
    }

    override fun onDisable() {
        this.adventure?.close()
        this.adventure = null
        this.tabListHandler.stopTabListTask()
        this.proxyPlugin.shutdown()
    }

    fun adventure(): BungeeAudiences {
        return adventure ?: throw IllegalStateException("Cannot retrieve audience provider while plugin is not enabled")
    }

    fun deserializeToComponent(text: String, player: ProxiedPlayer? = null): Component {
        val configureTagResolversEvent = this.proxy.pluginManager.callEvent(ConfigureTagResolversEvent(player))
        return this.miniMessage.deserialize(
            text,
            *configureTagResolversEvent.tagResolvers.toTypedArray()
        )
    }
}
