package app.simplecloud.plugin.proxy.shared.config.state

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class WhitelistConfig(
    val enabled: Boolean = true,
    val players: List<String> = listOf("Notch")
)
