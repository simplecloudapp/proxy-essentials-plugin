package app.simplecloud.plugin.proxy.shared.config.message

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class MessageVariables(
    val prefix: String = "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>"
)
