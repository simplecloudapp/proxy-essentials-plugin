package app.simplecloud.plugin.proxy.bungeecord

import app.simplecloud.plugin.api.bungeecord.command.BungeeCloudSender
import app.simplecloud.plugin.api.bungeecord.command.BungeeCloudSenderMapper
import app.simplecloud.plugin.proxy.bungeecord.listener.ConnectionListener
import app.simplecloud.plugin.proxy.bungeecord.listener.ProxyPingListener
import app.simplecloud.plugin.proxy.bungeecord.platform.BungeePlatformImpl
import app.simplecloud.plugin.proxy.shared.ProxyEssentials
import app.simplecloud.plugin.proxy.shared.command.ProxyCommandHandler
import net.kyori.adventure.platform.bungeecord.BungeeAudiences
import net.md_5.bungee.api.plugin.Plugin
import org.incendo.cloud.bungee.BungeeCommandManager
import org.incendo.cloud.execution.ExecutionCoordinator

class BungeeProxyPlugin : Plugin() {

    private val essentials = ProxyEssentials(BungeePlatformImpl(proxy, dataFolder.toPath()))
    private val audiences = BungeeAudiences.create(this)

    override fun onEnable() {
        essentials.start()

        proxy.pluginManager.registerListener(this, ConnectionListener(essentials))
        proxy.pluginManager.registerListener(this, ProxyPingListener(essentials))

        ProxyCommandHandler(createCommandManager(), essentials).register()
    }

    override fun onDisable() {
        essentials.stop()
        audiences.close()
    }

    private fun createCommandManager(): BungeeCommandManager<BungeeCloudSender> {
        return BungeeCommandManager(
            this,
            ExecutionCoordinator.asyncCoordinator(),
            BungeeCloudSenderMapper(audiences)
        )
    }
}
