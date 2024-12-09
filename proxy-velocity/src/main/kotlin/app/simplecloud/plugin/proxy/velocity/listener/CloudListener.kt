package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.event.velocity.mapping.CloudServerUpdateEvent
import app.simplecloud.plugin.proxy.shared.handler.JoinStateHandler
import app.simplecloud.plugin.proxy.velocity.ProxyVelocityPlugin
import com.velocitypowered.api.event.Subscribe
import kotlinx.coroutines.runBlocking
import java.util.logging.Logger

class CloudListener(
    private val plugin: ProxyVelocityPlugin
) {

    private val logger = Logger.getLogger(CloudListener::class.java.name)

    @Subscribe
    fun test(event: CloudServerUpdateEvent) {

        if (event.getTo().uniqueId != System.getenv("SIMPLECLOUD_UNIQUE_ID")) return

        checkStateChance(event)
    }

    private fun checkStateChance(event: CloudServerUpdateEvent) {
        val state = event.getTo().properties[JoinStateHandler.JOINSTATE_KEY]

        if (state == null) {
            this.logger.warning("No join state found for server. Using default join state.")

            runBlocking {
                plugin.joinStateHandler.setJoinStateAtGroupAndAllServicesInGroup(event.getTo().group, plugin.joinStateConfiguration.defaultState)
                plugin.joinStateHandler.localState = plugin.joinStateConfiguration.defaultState
            }
            return
        }

        if (state == event.getFrom().properties[JoinStateHandler.JOINSTATE_KEY]) return

        plugin.joinStateHandler.localState = state

        this.logger.info("Join state changed to: $state")
    }
}