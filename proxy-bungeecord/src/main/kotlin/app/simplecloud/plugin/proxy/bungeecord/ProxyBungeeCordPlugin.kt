package app.simplecloud.plugin.proxy.bungeecord

import app.simplecloud.plugin.proxy.bungeecord.event.ConfigureTagResolversEvent
import app.simplecloud.plugin.proxy.bungeecord.handler.TabListHandler
import app.simplecloud.plugin.proxy.bungeecord.listener.*
import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import app.simplecloud.plugin.proxy.shared.handler.command.CommandSender
import app.simplecloud.plugin.proxy.shared.handler.command.JoinStateCommandHandler
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

    private lateinit var commandManager: BungeeCommandManager<CommandSender>

    private var adventure: BungeeAudiences? = null

    private val miniMessage = MiniMessage.miniMessage()

    override fun onEnable() {
        this.proxyPlugin.config.save("tablist", this.proxyPlugin.tabListConfiguration)
        this.proxyPlugin.config.save("placeholder", this.proxyPlugin.placeHolderConfiguration)
        this.proxyPlugin.config.save("messages", this.proxyPlugin.messagesConfiguration)
        this.proxyPlugin.config.save("joinstate", this.proxyPlugin.joinStateConfiguration)

        this.proxyPlugin.motdLayoutHandler.loadMotdLayouts()

        this.adventure = BungeeAudiences.create(this)
        this.proxy.pluginManager.registerListener(this, ProxyPingListener(this))
        this.proxy.pluginManager.registerListener(this, ConfigureTagResolversListener(this))
        this.proxy.pluginManager.registerListener(this, ServerPreConnectListener(this))

        if (this.proxyPlugin.tabListConfiguration.get().tabListUpdateTime > 0)
            this.tabListHandler.startTabListTask()
        else
            this.logger.info("Tablist update time is set to 0, tablist will not be updated automatically")

        val executionCoordinator = ExecutionCoordinator.simpleCoordinator<CommandSender>()

        val senderMapper = SenderMapper.create<net.md_5.bungee.api.CommandSender, CommandSender>(
            { commandSender -> BungeeCordCommandSender(commandSender) },
            { cloudSender -> (cloudSender as BungeeCordCommandSender).getCommandSender() }
        )

        commandManager = BungeeCommandManager(
            this,
            executionCoordinator,
            senderMapper
        )

        val proxyCommandHandler = JoinStateCommandHandler(commandManager, this.proxyPlugin)
        proxyCommandHandler.loadCommands()
    }

    override fun onDisable() {
        if(this.adventure != null) {
            this.adventure!!.close()
            this.adventure = null
        }

        this.tabListHandler.stopTabListTask()
    }

    fun adventure(): BungeeAudiences {
        checkNotNull(this.adventure) { "Cannot retrieve audience provider while plugin is not enabled" }
        return adventure!!
    }

    fun deserializeToComponent(text: String, player: ProxiedPlayer? = null): Component {
        val configureTagResolversEvent = this.proxy.pluginManager.callEvent(ConfigureTagResolversEvent(player))
        return this.miniMessage.deserialize(
            text,
            *configureTagResolversEvent.tagResolvers.toTypedArray()
        )
    }
}