package app.simplecloud.plugin.proxy.shared.config.message

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class MessageConfig(
    val version: String = "1",
    val variables: MessageVariables = MessageVariables(),
    val kick: KickMessageConfig = KickMessageConfig(),
    val command: CommandMessages = CommandMessages()
) {
    fun resolve(message: String): String = message.replace("<prefix>", variables.prefix)
}