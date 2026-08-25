package app.simplecloud.plugin.proxy.shared.config

import app.simplecloud.plugin.proxy.shared.utilities.config.DefaultConfigs
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class PlaceHolderConfiguration(
    val currentDateFormat: String = "dd.MM.yyyy",
    val currentTimeFormat: String = "HH:mm:ss",
    val pingColors: List<PingColor> = DefaultConfigs.PING_COLORS
)

@ConfigSerializable
data class PingColor(
    val ping: Int,
    val color: String
)
