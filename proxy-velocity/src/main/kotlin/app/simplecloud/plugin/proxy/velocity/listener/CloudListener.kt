package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.event.velocity.mapping.CloudServerUpdateEvent
import app.simplecloud.plugin.proxy.velocity.ProxyVelocityPlugin
import com.velocitypowered.api.event.Subscribe

class CloudListener(
    private val plugin: ProxyVelocityPlugin
) {

    @Subscribe
    fun test(event: CloudServerUpdateEvent) {

        if (event.getTo().uniqueId != System.getenv("SIMPLECLOUD_UNIQUE_ID")) return

        val isMaintenance = event.getTo().properties["maintenance"]

        if (isMaintenance == event.getFrom().properties["maintenance"]) return

        val newMaintenanceState = isMaintenance == "true"

        if (this.plugin.maintenance == newMaintenanceState) return

        this.plugin.maintenance = newMaintenanceState
    }
}