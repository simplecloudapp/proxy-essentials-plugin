package app.simplecloud.plugin.proxy.velocity.listener

import app.simplecloud.event.velocity.mapping.CloudServerUpdateEvent
import app.simplecloud.plugin.proxy.shared.handler.MotdLayoutHandler
import app.simplecloud.plugin.proxy.velocity.ProxyVelocityPlugin
import com.velocitypowered.api.event.Subscribe
import java.util.logging.Logger

class CloudListener(
    private val plugin: ProxyVelocityPlugin
) {

    private val logger = Logger.getLogger(CloudListener::class.java.name)

    @Subscribe
    fun test(event: CloudServerUpdateEvent) {

        if (event.getTo().uniqueId != System.getenv("SIMPLECLOUD_UNIQUE_ID")) return

        checkMaintenanceChance(event)
        checkLayoutMaintenanceChance(event)
        checkLayoutChance(event)
    }

    private fun checkMaintenanceChance(event: CloudServerUpdateEvent) {
        val isMaintenance = event.getTo().properties["maintenance"]

        if (isMaintenance == event.getFrom().properties["maintenance"]) return

        val newMaintenanceState = isMaintenance == "true"

        if (this.plugin.maintenance == newMaintenanceState) return

        this.plugin.maintenance = newMaintenanceState

        this.logger.info("Maintenance mode has been toggled to $newMaintenanceState")
    }

    private fun checkLayoutMaintenanceChance(event: CloudServerUpdateEvent) {
        val layout = event.getTo().properties[MotdLayoutHandler.CURRENT_MAINTENANCE_LAYOUT_KEY]

        if (layout == event.getFrom().properties[MotdLayoutHandler.CURRENT_MAINTENANCE_LAYOUT_KEY]) return

        val newLayout = layout ?: MotdLayoutHandler.DEFAULT_MAINTENANCE_LAYOUT_NAME

        if (MotdLayoutHandler.CURRENT_MAINTENANCE_LAYOUT_KEY == newLayout) return

        MotdLayoutHandler.CURRENT_MAINTENANCE_LAYOUT_KEY = newLayout

        this.logger.info("Layout has been changed to $newLayout")
    }

    private fun checkLayoutChance(event: CloudServerUpdateEvent) {
        val layout = event.getTo().properties[MotdLayoutHandler.CURRENT_LAYOUT_KEY]

        if (layout == event.getFrom().properties[MotdLayoutHandler.CURRENT_LAYOUT_KEY]) return

        val newLayout = layout ?: MotdLayoutHandler.DEFAULT_LAYOUT_NAME

        if (MotdLayoutHandler.CURRENT_LAYOUT_KEY == newLayout) return

        MotdLayoutHandler.CURRENT_LAYOUT_KEY = newLayout

        this.logger.info("Layout has been changed to $newLayout")
    }
}