package app.simplecloud.plugin.proxy.bungeecord.listener

import app.simplecloud.event.bungeecord.mapping.CloudServerUpdateEvent
import app.simplecloud.plugin.proxy.bungeecord.ProxyBungeeCordPlugin
import app.simplecloud.plugin.proxy.shared.handler.JoinStateHandler
import kotlinx.coroutines.runBlocking
import net.md_5.bungee.api.plugin.Listener
import net.md_5.bungee.event.EventHandler
import java.util.logging.Logger

class CloudListener(
    private val plugin: ProxyBungeeCordPlugin
) : Listener {

    private val logger = Logger.getLogger(CloudListener::class.java.name)

    @EventHandler
    fun onServerUpdate(event: CloudServerUpdateEvent) {
        if (event.getTo().uniqueId != System.getenv("SIMPLECLOUD_UNIQUE_ID")) return

        checkStateChance(event)
    }

    private fun checkStateChance(event: CloudServerUpdateEvent) {
        val state = event.getTo().properties[JoinStateHandler.JOINSTATE_KEY]

        if (state == null) {
            this.logger.warning("No join state found for server. Using default join state.")

            runBlocking {
                plugin.proxyPlugin.joinStateHandler.setJoinStateAtGroupAndAllServicesInGroup(
                    event.getTo().group,
                    plugin.proxyPlugin.joinStateConfiguration.defaultState
                )
                plugin.proxyPlugin.joinStateHandler.localState = plugin.proxyPlugin.joinStateConfiguration.defaultState
            }
            return
        }

        if (state == event.getFrom().properties[JoinStateHandler.JOINSTATE_KEY]) return

        runBlocking {
            plugin.proxyPlugin.joinStateHandler.setJoinStateAtGroupAndAllServicesInGroup(event.getTo().group, state)
            plugin.proxyPlugin.joinStateHandler.localState = state
        }

        this.logger.info("Join state changed to: $state")
    }
}