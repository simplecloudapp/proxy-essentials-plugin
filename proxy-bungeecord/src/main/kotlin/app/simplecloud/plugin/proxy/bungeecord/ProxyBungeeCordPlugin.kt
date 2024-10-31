package app.simplecloud.plugin.proxy.bungeecord

import app.simplecloud.plugin.proxy.bungeecord.event.ConfigureTagResolversEvent
import app.simplecloud.plugin.proxy.bungeecord.handler.TabListHandler
import app.simplecloud.plugin.proxy.bungeecord.listener.ConfigureTagResolversListener
import app.simplecloud.plugin.proxy.bungeecord.listener.ProxyPingListener
import app.simplecloud.plugin.proxy.bungeecord.listener.TabListListener
import app.simplecloud.plugin.proxy.shared.config.YamlConfig
import app.simplecloud.plugin.proxy.shared.config.placeholder.PlaceHolderConfiguration
import app.simplecloud.plugin.proxy.shared.config.tablis.TabListConfiguration
import app.simplecloud.plugin.proxy.shared.handler.MotdLayoutHandler
import net.kyori.adventure.platform.bungeecord.BungeeAudiences
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Plugin


class ProxyBungeeCordPlugin: Plugin() {

    lateinit var generalConfiguration: GeneralConfig
    lateinit var tabListConfiguration: TabListConfiguration
    lateinit var placeHolderConfiguration: PlaceHolderConfiguration

    val config = YamlConfig(this.dataFolder.path)
    val tabListHandler = TabListHandler(this)
    val motdLayoutHandler = MotdLayoutHandler(config, generalConfiguration)

    private var adventure: BungeeAudiences? = null

    private val miniMessage = MiniMessage.miniMessage()

    override fun onEnable() {
        val config = YamlConfig(this.dataFolder.path)

        this.generalConfiguration = config.load<GeneralConfig>("general")!!
        config.save("general", this.generalConfiguration)

        this.tabListConfiguration = config.load<TabListConfiguration>("tablist")!!
        config.save("tablist", this.tabListConfiguration)

        this.placeHolderConfiguration = config.load<PlaceHolderConfiguration>("placeholder")!!
        config.save("placeholder", this.placeHolderConfiguration)

        this.motdLayoutHandler.loadMotdLayouts()

        this.adventure = BungeeAudiences.create(this);

        if (this.tabListConfiguration.tabListUpdateTime > 0)
            this.tabListHandler.startTabListTask()
        else
            this.logger.info("Tablist update time is set to 0, tablist will not be updated automatically")

        this.proxy.pluginManager.registerListener(this, TabListListener(this))
        this.proxy.pluginManager.registerListener(this, ProxyPingListener(this))
        this.proxy.pluginManager.registerListener(this, ConfigureTagResolversListener(this))
    }

    override fun onDisable() {
        if(this.adventure != null) {
            this.adventure!!.close();
            this.adventure = null;
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