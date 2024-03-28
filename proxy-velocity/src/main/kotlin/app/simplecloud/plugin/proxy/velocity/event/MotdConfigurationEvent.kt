package app.simplecloud.plugin.proxy.velocity.event

import app.simplecloud.plugin.proxy.shared.event.MotdConfiguration

class MotdConfigurationEvent(
    var pingConfiguration: MotdConfiguration
) {
}