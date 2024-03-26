package app.simplecloud.plugin.proxy.velocity.handler

import app.simplecloud.plugin.proxy.shared.config.tablis.TabList
import app.simplecloud.plugin.proxy.shared.config.tablis.TabListGroup
import app.simplecloud.plugin.proxy.shared.event.TabListPlayerConfiguration
import app.simplecloud.plugin.proxy.velocity.ProxyVelocityPlugin
import app.simplecloud.plugin.proxy.velocity.event.TabListConfigurationEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.scheduler.ScheduledTask
import net.kyori.adventure.text.minimessage.MiniMessage

class TabListHandler(
    private val plugin: ProxyVelocityPlugin
) {

    private val miniMessage = MiniMessage.miniMessage()

    private val tabListIndex = mutableMapOf<String, Int>()

    private lateinit var task: ScheduledTask

    fun startTabListTask() {
        this.task = this.plugin.proxyServer.scheduler.buildTask(plugin) {
            this.plugin.proxyServer.allPlayers.forEach {
                this.updateTabListForPlayer(it)
            }
        }.repeat(this.plugin.tabListConfiguration.tabListUpdateTime, java.util.concurrent.TimeUnit.MILLISECONDS).schedule()
    }

    fun stopTabListTask() {
        this.task.cancel()
    }

    fun updateTabListForPlayer(player: Player) {
        val configuration = plugin.tabListConfiguration

        if (player.currentServer.isEmpty)
            return

        val serviceName = player.currentServer.get().serverInfo.name

        var tabListGroup = configuration.groups.find { group ->
            group.groupOrService == "*"
        }

        if (tabListGroup == null) {
            tabListGroup = configuration.groups.find { group ->
                serviceName.startsWith(group.groupOrService, true)
            }
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
            tabListGroup = TabListGroup(
                "noValuedGroupOrServiceConfigurationsFround",
                listOf(TabList(
                    listOf("<red>No configuration found for this service"),
                    listOf("<red>Please check your configuration file from the proxy plugin in the plugins folder"),
                ))
            )
        }

        val tabList = tabListGroup.tabLists[tabListIndex.getOrDefault(tabListGroup.groupOrService, 0)]
        if (tabListGroup.tabLists.size <= tabListIndex.getOrDefault(tabListGroup.groupOrService, 0) + 1)
            tabListIndex[tabListGroup.groupOrService] = 0


        val header = this.miniMessage.deserialize(tabList.header.joinToString("<newline>"))
        val footer = this.miniMessage.deserialize(tabList.footer.joinToString("<newline>"))

        val tabListPlayerConfiguration = this.plugin.proxyServer.eventManager.fire(
            TabListConfigurationEvent(
                TabListPlayerConfiguration(
                    header,
                    footer
                ), player
            )
        ).join().tabListConfiguration


        player.sendPlayerListHeaderAndFooter(
            tabListPlayerConfiguration.header,
            tabListPlayerConfiguration.footer
        )
    }
}