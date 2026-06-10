package app.simplecloud.plugin.proxy.shared.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class PlayerCountConfig(
    @Setting("additional-groups") val additionalGroups: List<String> = emptyList(),
    @Setting("additional-persistent-servers") val additionalPersistentServers: List<String> = emptyList(),
    @Setting("update-time") val updateTime: Long = 20L
)
