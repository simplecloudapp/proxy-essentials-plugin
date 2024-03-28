package app.simplecloud.plugin.proxy.shared.config.placeholder

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class PlaceHolderConfiguration(
    val currentDateFormat: String = "dd.MM.yyyy",
    val currentTimeFormat: String = "HH:mm:ss",
    val pingColors: List<PingColor> = listOf(
        PingColor(0, "<green>"),
        PingColor(50, "<yellow>"),
        PingColor(100, "<gold>"),
        PingColor(150, "<red>"),
        PingColor(200, "<dark_red>")
    )
)