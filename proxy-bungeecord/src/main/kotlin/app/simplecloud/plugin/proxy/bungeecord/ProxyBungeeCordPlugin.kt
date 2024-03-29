package app.simplecloud.plugin.proxy.bungeecord

import app.simplecloud.plugin.proxy.shared.config.YamlConfig
import app.simplecloud.plugin.proxy.shared.config.motd.MotdConfiguration
import app.simplecloud.plugin.proxy.shared.config.placeholder.PlaceHolderConfiguration
import app.simplecloud.plugin.proxy.shared.config.tablis.TabListConfiguration
import net.md_5.bungee.api.plugin.Plugin
import kotlin.io.path.pathString

class ProxyBungeeCordPlugin: Plugin() {

    lateinit var motdConfiguration: MotdConfiguration
    lateinit var tabListConfiguration: TabListConfiguration
    lateinit var placeHolderConfiguration: PlaceHolderConfiguration

    override fun onEnable() {
        val config = YamlConfig(this.dataFolder.path)

        this.motdConfiguration = config.load<MotdConfiguration>("motd")!!
        config.save("motd", this.motdConfiguration)

        this.tabListConfiguration = config.load<TabListConfiguration>("tablist")!!
        config.save("tablist", this.tabListConfiguration)

        this.placeHolderConfiguration = config.load<PlaceHolderConfiguration>("placeholder")!!
        config.save("placeholder", this.placeHolderConfiguration)
    }

    override fun onDisable() {

    }
}