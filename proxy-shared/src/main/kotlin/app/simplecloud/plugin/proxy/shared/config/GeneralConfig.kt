package app.simplecloud.plugin.proxy.shared.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class GeneralConfig(
    var currentLayout: String = "default-motd"
)