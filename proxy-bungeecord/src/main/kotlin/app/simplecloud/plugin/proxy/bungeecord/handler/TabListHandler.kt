package app.simplecloud.plugin.proxy.bungeecord.handler

import app.simplecloud.plugin.proxy.bungeecord.ProxyBungeeCordPlugin
import app.simplecloud.plugin.proxy.bungeecord.event.TabListConfigurationEvent
import app.simplecloud.plugin.proxy.shared.config.tablis.TabList
import app.simplecloud.plugin.proxy.shared.config.tablis.TabListGroup
import app.simplecloud.plugin.proxy.shared.event.TabListPlayerConfiguration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.scheduler.ScheduledTask
import java.util.concurrent.TimeUnit


class TabListHandler(
    private val plugin: ProxyBungeeCordPlugin
) {

    private val miniMessage = MiniMessage.miniMessage()

    private val tabListIndex = mutableMapOf<String, Int>()

    private lateinit var task: ScheduledTask

    fun startTabListTask() {
        this.task = this.plugin.proxy.scheduler.schedule(this.plugin, {
            this.plugin.proxy.players.forEach {
                this.updateTabListForPlayer(it)
            }
            this.tabListIndex.forEach { (key, value) ->
                this.tabListIndex[key] = value + 1
            }
        }, 1, this.plugin.tabListConfiguration.tabListUpdateTime, TimeUnit.MILLISECONDS)
    }

    fun stopTabListTask() {
        if (!this::task.isInitialized)
            return

        this.task.cancel()
    }

    fun updateTabListForPlayer(player: ProxiedPlayer) {
        val configuration = plugin.tabListConfiguration

        val serviceName = player.server.info.name

        var tabListGroup = configuration.groups.find { group ->
            serviceName.startsWith(group.groupOrService, true)
        }

        if (tabListGroup == null) {
            tabListGroup = configuration.groups.find { group ->
                group.groupOrService.equals(
                    serviceName,
                    true
                )
            }
        }

        if (tabListGroup == null) {
            tabListGroup = configuration.groups.find { group ->
                group.groupOrService == "*"
            }
        }

        if (tabListGroup == null) {
            tabListGroup = TabListGroup(
                "noValuedGroupOrServiceConfigurationsFround",
                listOf(
                    TabList(
                    listOf("<red>No configuration found for this service"),
                    listOf("<red>Please check your configuration file from the proxy plugin in the plugins folder"),
                )
                )
            )
        }

        if (tabListGroup.tabLists.size <= tabListIndex.getOrDefault(tabListGroup.groupOrService, 0) + 1)
            tabListIndex[tabListGroup.groupOrService] = 0

        if (!tabListIndex.containsKey(tabListGroup.groupOrService))
            tabListIndex[tabListGroup.groupOrService] = 0

        val tabList = tabListGroup.tabLists[tabListIndex.getOrDefault(tabListGroup.groupOrService, 0)]

        val header = this.miniMessage.deserialize(tabList.header.joinToString("<newline>"))
        val footer = this.miniMessage.deserialize(tabList.footer.joinToString("<newline>"))

        val tabListPlayerConfiguration = this.plugin.proxy.pluginManager.callEvent(
            TabListConfigurationEvent(
                TabListPlayerConfiguration(
                    header,
                    footer
                ), player
            )
        ).tabListConfiguration

        val audience = this.plugin.adventure().player(player)
        audience.sendPlayerListHeaderAndFooter(
            tabListPlayerConfiguration.header,
            tabListPlayerConfiguration.footer
        )
    }
}