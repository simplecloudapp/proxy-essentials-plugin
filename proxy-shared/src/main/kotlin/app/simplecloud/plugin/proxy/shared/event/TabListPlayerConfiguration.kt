package app.simplecloud.plugin.proxy.shared.event

import net.kyori.adventure.text.Component

data class TabListPlayerConfiguration(
    var header: Component,
    var footer: Component,
) {
}