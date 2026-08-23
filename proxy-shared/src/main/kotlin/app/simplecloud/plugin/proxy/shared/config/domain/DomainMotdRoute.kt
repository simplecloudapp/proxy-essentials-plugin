package app.simplecloud.plugin.proxy.shared.config.domain

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class DomainMotdRule(
    val state: String = "",
    val layout: String = ""
)

@ConfigSerializable
data class DomainMotdRoute(
    val domain: String = "",
    val target: String = "",
    val rules: List<DomainMotdRule> = listOf()
)
