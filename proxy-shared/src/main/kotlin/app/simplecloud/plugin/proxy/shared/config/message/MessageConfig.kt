package app.simplecloud.plugin.proxy.shared.config.message

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class MessageConfig(
    var version: String = "1",
    var kickMessage: KickMessageConfig = KickMessageConfig(),
    var commandMessage: CommandMessageConfig = CommandMessageConfig()
)