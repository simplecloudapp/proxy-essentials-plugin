package app.simplecloud.plugin.proxy.bungeecord

import app.simplecloud.plugin.proxy.bungeecord.command.BungeeCordCommandSender
import app.simplecloud.plugin.proxy.bungeecord.listener.PostLoginListener
import app.simplecloud.plugin.proxy.bungeecord.listener.ProxyPingListener
import app.simplecloud.plugin.proxy.bungeecord.listener.ServerKickListener
import app.simplecloud.plugin.proxy.bungeecord.listener.ServerPreConnectListener
import app.simplecloud.plugin.proxy.bungeecord.listener.TabListListener
import app.simplecloud.plugin.proxy.bungeecord.platform.BungeeCordPlatformImpl
import app.simplecloud.plugin.proxy.shared.ProxyEssentials
import app.simplecloud.plugin.proxy.shared.command.ProxyCommandHandler
import net.md_5.bungee.api.plugin.Plugin
import org.incendo.cloud.SenderMapper
import org.incendo.cloud.bungee.BungeeCommandManager
import org.incendo.cloud.execution.ExecutionCoordinator

class ProxyBungeeCordPlugin : Plugin() {

    private val essentials = ProxyEssentials(BungeeCordPlatformImpl(proxy, dataFolder.toPath()))
    private val tabListHandler = TabListHandler(this, essentials)

    override fun onEnable() {
        essentials.start()

        proxy.pluginManager.registerListener(this, PostLoginListener(essentials))
        proxy.pluginManager.registerListener(this, ServerPreConnectListener(essentials))
        proxy.pluginManager.registerListener(this, ProxyPingListener(essentials))
        proxy.pluginManager.registerListener(this, ServerKickListener(essentials))
        proxy.pluginManager.registerListener(this, TabListListener(tabListHandler))

        ProxyCommandHandler(createCommandManager(), essentials).register()
        tabListHandler.start()
    }

    override fun onDisable() {
        proxy.scheduler.cancel(this)
        essentials.stop()
    }

    private fun createCommandManager(): BungeeCommandManager<BungeeCordCommandSender> {
        return BungeeCommandManager(
            this,
            ExecutionCoordinator.asyncCoordinator(),
            SenderMapper.create(::BungeeCordCommandSender, BungeeCordCommandSender::commandSender)
        )
    }
}
