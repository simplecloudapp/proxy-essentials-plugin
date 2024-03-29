package app.simplecloud.plugin.proxy.bungeecord

import app.simplecloud.plugin.proxy.bungeecord.handler.TabListHandler
import app.simplecloud.plugin.proxy.bungeecord.listener.TabListListener
import app.simplecloud.plugin.proxy.shared.config.YamlConfig
import app.simplecloud.plugin.proxy.shared.config.motd.MotdConfiguration
import app.simplecloud.plugin.proxy.shared.config.placeholder.PlaceHolderConfiguration
import app.simplecloud.plugin.proxy.shared.config.tablis.TabListConfiguration
import net.kyori.adventure.platform.bungeecord.BungeeAudiences
import net.md_5.bungee.api.plugin.Plugin


class ProxyBungeeCordPlugin: Plugin() {

    lateinit var motdConfiguration: MotdConfiguration
    lateinit var tabListConfiguration: TabListConfiguration
    lateinit var placeHolderConfiguration: PlaceHolderConfiguration

    val tabListHandler = TabListHandler(this)

    private var adventure: BungeeAudiences? = null

    override fun onEnable() {
        val config = YamlConfig(this.dataFolder.path)

        this.motdConfiguration = config.load<MotdConfiguration>("motd")!!
        config.save("motd", this.motdConfiguration)

        this.tabListConfiguration = config.load<TabListConfiguration>("tablist")!!
        config.save("tablist", this.tabListConfiguration)

        this.placeHolderConfiguration = config.load<PlaceHolderConfiguration>("placeholder")!!
        config.save("placeholder", this.placeHolderConfiguration)

        this.adventure = BungeeAudiences.create(this);

        this.tabListHandler.startTabListTask()

        this.proxy.pluginManager.registerListener(this, TabListListener(this))
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
}