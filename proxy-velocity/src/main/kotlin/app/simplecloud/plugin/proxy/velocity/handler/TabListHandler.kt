package app.simplecloud.plugin.proxy.velocity.handler

import app.simplecloud.plugin.proxy.shared.config.tablis.TabList
import app.simplecloud.plugin.proxy.shared.config.tablis.TabListGroup
import app.simplecloud.plugin.proxy.velocity.ProxyVelocityPlugin
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.scheduler.ScheduledTask

class TabListHandler(
    private val plugin: ProxyVelocityPlugin
) {

    private val tabListIndex = mutableMapOf<String, Int>()

    private lateinit var task: ScheduledTask

    fun startTabListTask() {
        this.task = this.plugin.proxyServer.scheduler.buildTask(plugin, Runnable {
            this.plugin.proxyServer.allPlayers.forEach {
                this.updateTabListForPlayer(it)
            }
            this.tabListIndex.forEach { (key, value) ->
                this.tabListIndex[key] = value + 1
            }
        }).repeat(this.plugin.tabListConfiguration.tabListUpdateTime, java.util.concurrent.TimeUnit.MILLISECONDS).schedule()
    }

    fun stopTabListTask() {
        if (!this::task.isInitialized) {
            println("Can't stop tablist task because it is not initialized")
            return
        }

        this.task.cancel()
    }

    fun updateTabListForPlayer(player: Player) {
        val configuration = plugin.tabListConfiguration

        if (player.currentServer.isEmpty)
            return

        val serviceName = player.currentServer.get().serverInfo.name

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
                listOf(TabList(
                    listOf("<red>No configuration found for this service"),
                    listOf("<red>Please check your configuration file from the proxy plugin in the plugins folder"),
                ))
            )
        }

        if (tabListGroup.tabLists.size <= tabListIndex.getOrDefault(tabListGroup.groupOrService, 0) + 1)
            tabListIndex[tabListGroup.groupOrService] = 0

        if (!tabListIndex.containsKey(tabListGroup.groupOrService))
            tabListIndex[tabListGroup.groupOrService] = 0

        val tabList = tabListGroup.tabLists[tabListIndex.getOrDefault(tabListGroup.groupOrService, 0)]

        val header = (tabList.header.joinToString("<newline>"))
        val footer = (tabList.footer.joinToString("<newline>"))

        player.sendPlayerListHeaderAndFooter(
            this.plugin.deserializeToComponent(header, player),
            this.plugin.deserializeToComponent(footer, player),
        )
    }

}