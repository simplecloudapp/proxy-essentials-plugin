package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.event.velocity.mapping.CloudServerUpdateEvent
import app.simplecloud.plugin.proxy.shared.handler.JoinStateHandler
import app.simplecloud.plugin.proxy.velocity.ProxyVelocityPlugin
import com.velocitypowered.api.event.Subscribe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.logging.Logger

class CloudListener(
    private val plugin: ProxyVelocityPlugin
) {

    private val logger = Logger.getLogger(CloudListener::class.java.name)

    @Subscribe
    fun onServerUpdate(event: CloudServerUpdateEvent) {
        if (event.getTo().uniqueId != System.getenv("SIMPLECLOUD_UNIQUE_ID")) return

        checkStateChanged(event)
    }

    private fun checkStateChanged(event: CloudServerUpdateEvent) {
        val state = event.getTo().properties[JoinStateHandler.JOINSTATE_KEY]

        if (state == null) {
            this.logger.warning("No join state found for server. Using default join state.")

            CoroutineScope(Dispatchers.IO).launch {
                plugin.joinStateHandler.setJoinStateAtGroupAndAllServicesInGroup(event.getTo().group, plugin.joinStateConfiguration.defaultState)
                plugin.joinStateHandler.localState = plugin.joinStateConfiguration.defaultState
            }
            return
        }

        if (state == plugin.joinStateHandler.localState) return

        plugin.joinStateHandler.localState = state
        this.logger.info("Join state changed to $state")
    }
}