package app.simplecloud.plugin.proxy.velocity

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.plugin.proxy.shared.ProxyPlugin
import app.simplecloud.plugin.proxy.shared.config.YamlConfig
import app.simplecloud.plugin.proxy.shared.config.placeholder.PlaceHolderConfiguration
import app.simplecloud.plugin.proxy.shared.config.tablis.TabListConfiguration
import app.simplecloud.plugin.proxy.shared.handler.MotdLayoutHandler
import app.simplecloud.plugin.proxy.velocity.event.ConfigureTagResolversEvent
import app.simplecloud.plugin.proxy.velocity.handler.TabListHandler
import app.simplecloud.plugin.proxy.velocity.listener.CloudListener
import app.simplecloud.plugin.proxy.velocity.listener.ConfigureTagResolversListener
import app.simplecloud.plugin.proxy.velocity.listener.ProxyPingListener
import app.simplecloud.plugin.proxy.velocity.listener.ServerPreConnectListener
import com.google.inject.Inject
import com.velocitypowered.api.command.RawCommand
import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.command.SimpleCommand.Invocation
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.slf4j.Logger
import java.nio.file.Path
import kotlin.io.path.pathString

class ProxyVelocityPlugin @Inject constructor(
    val proxyServer: ProxyServer,
    @DataDirectory val dataDirectory: Path,
    val logger: Logger
): ProxyPlugin(dataDirectory.pathString) {

    val tabListHandler = TabListHandler(this)

    private val miniMessage = MiniMessage.miniMessage()

    @Subscribe
    fun onProxyInitialize(event: ProxyInitializeEvent) {
        config.save("tablist", this.tabListConfiguration)
        config.save("placeholder", this.placeHolderConfiguration)
        config.save("messages", this.messagesConfiguration)

        this.motdLayoutHandler.loadMotdLayouts()

        this.proxyServer.eventManager.register(this, ProxyPingListener(this))
        this.proxyServer.eventManager.register(this, ConfigureTagResolversListener(this))
        this.proxyServer.eventManager.register(this, CloudListener(this))
        this.proxyServer.eventManager.register(this, ServerPreConnectListener(this))

        if (this.tabListConfiguration.tabListUpdateTime > 0)
            this.tabListHandler.startTabListTask()
        else
            this.logger.info("Tablist update time is set to 0, tablist will not be updated automatically")

        val commandMeta = this.proxyServer.commandManager.metaBuilder("test-maintenance").build()

        System.getenv("SIMPLECLOUD_MAINTENANCE")?.let {
            this.maintenance = it == "true"
        }
    }

    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        this.tabListHandler.stopTabListTask()
    }

    fun deserializeToComponent(text: String, player: Player? = null): Component {
        val configureTagResolversEvent = this.proxyServer.eventManager.fire(ConfigureTagResolversEvent(player)).get()
        return this.miniMessage.deserialize(
            text,
            *configureTagResolversEvent.tagResolvers.toTypedArray()
        )
    }

}