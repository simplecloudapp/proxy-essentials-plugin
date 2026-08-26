package app.simplecloud.plugin.proxy.bungeecord

import app.simplecloud.plugin.api.shared.extension.miniMessage
import app.simplecloud.plugin.proxy.bungeecord.listener.PostLoginListener
import app.simplecloud.plugin.proxy.bungeecord.listener.ServerKickListener
import app.simplecloud.plugin.proxy.bungeecord.listener.ServerPreConnectListener
import app.simplecloud.plugin.proxy.bungeecord.listener.ProxyPingListener
import app.simplecloud.plugin.proxy.bungeecord.tablist.TabListHandler
import app.simplecloud.plugin.proxy.bungeecord.tablist.TabListListener
import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import app.simplecloud.plugin.proxy.shared.command.ProxyCommandSender
import app.simplecloud.plugin.proxy.shared.command.ProxyEssentialsCommandHandler
import app.simplecloud.plugin.proxy.shared.command.commands.JoinStateCommandHandler
import app.simplecloud.plugin.proxy.shared.command.commands.LayoutCommandHandler
import app.simplecloud.plugin.proxy.shared.placeholder.TagResolverHelper
import app.simplecloud.plugin.proxy.shared.utilities.format.MotdMiniMessageFormatter
import net.kyori.adventure.platform.bungeecord.BungeeAudiences
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Plugin
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.bungee.BungeeCommandManager
import org.incendo.cloud.execution.ExecutionCoordinator

class ProxyBungeeCordPlugin : Plugin() {

    val proxyPlugin = ProxyPlugin(dataFolder.path)
    val tabListHandler = TabListHandler(this)

    private var adventure: BungeeAudiences? = null

    override fun onEnable() {
        adventure = BungeeAudiences.create(this)
        proxyPlugin.start()

        registerListeners()
        registerCommands()
        startTabListTask()
    }

    override fun onDisable() {
        adventure?.close()
        adventure = null
        tabListHandler.stopTabListTask()
        proxyPlugin.shutdown()
    }

    fun adventure(): BungeeAudiences {
        return adventure ?: error("Cannot retrieve audience provider while plugin is not enabled")
    }

    fun deserializeToComponent(text: String, player: ProxiedPlayer? = null): Component {
        return miniMessage.deserialize(text, *tagResolvers(player).toTypedArray())
    }

    fun deserializeMotd(line1: String, line2: String): Component {
        return MotdMiniMessageFormatter.deserialize(miniMessage, line1, line2, tagResolvers(null))
    }

    private fun registerListeners() {
        val manager = proxy.pluginManager

        manager.registerListener(this, ProxyPingListener(this))
        manager.registerListener(this, PostLoginListener(this))
        manager.registerListener(this, ServerPreConnectListener(this))
        manager.registerListener(this, ServerKickListener(proxyPlugin))
        manager.registerListener(this, TabListListener(this))
    }

    private fun registerCommands() {
        val commandManager = BungeeCommandManager(
            this,
            ExecutionCoordinator.asyncCoordinator(),
            SenderMapper.create<CommandSender, ProxyCommandSender>(
                { commandSender -> BungeeCordCommandSender(commandSender, adventure) },
                { cloudSender -> (cloudSender as BungeeCordCommandSender).commandSender }
            )
        )

        ProxyEssentialsCommandHandler(commandManager, proxyPlugin).loadCommands()
        JoinStateCommandHandler(commandManager, proxyPlugin).loadCommands()
        LayoutCommandHandler(commandManager, proxyPlugin).loadCommands()
    }

    private fun startTabListTask() {
        if (proxyPlugin.config.get().tabListUpdateTimeMillis() <= 0) {
            logger.info("Tablist update time is set to 0, tablist will not be updated automatically")
            return
        }

        tabListHandler.startTabListTask()
    }

    private fun tagResolvers(player: ProxiedPlayer?): List<TagResolver> {
        val playerCountTracker = proxyPlugin.playerCountTracker

        return TagResolverHelper.getDefaultTagResolvers(
            serverName = player?.server?.info?.name ?: "unknown",
            ping = (player?.ping ?: -1).toLong(),
            pingColors = proxyPlugin.placeholderConfig.get().pingColors,
            onlinePlayers = playerCountTracker.onlinePlayers(proxy.players.size),
            realMaxPlayers = playerCountTracker.maxPlayers(proxy.config.playerLimit),
            motdConfiguration = proxyPlugin.layoutRepository.getCurrentMotdLayout()
        )
    }
}
