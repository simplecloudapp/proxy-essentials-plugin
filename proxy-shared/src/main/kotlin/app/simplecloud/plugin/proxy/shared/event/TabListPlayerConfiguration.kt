package app.simplecloud.plugin.proxy.shared.event

import net.kyori.adventure.text.Component

data class TabListPlayerConfiguration(
    val header: Component,
    val footer: Component,
) {
}