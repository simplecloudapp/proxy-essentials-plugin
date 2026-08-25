package app.simplecloud.plugin.proxy.shared.utilities.config

import app.simplecloud.plugin.proxy.shared.config.JoinState
import app.simplecloud.plugin.proxy.shared.config.JoinStatePermission
import app.simplecloud.plugin.proxy.shared.config.PingColor
import app.simplecloud.plugin.proxy.shared.config.TabList
import app.simplecloud.plugin.proxy.shared.config.TabListGroup

object DefaultConfigs {

    val VARIABLES: Map<String, String> = mapOf("prefix" to "<color:#38bdf8><bold>⚡</bold></color> <color:#ffffff>")

    val PING_COLORS: List<PingColor> = listOf(
        PingColor(0, "<green>"),
        PingColor(50, "<yellow>"),
        PingColor(100, "<gold>"),
        PingColor(150, "<red>"),
        PingColor(200, "<dark_red>")
    )

    val JOIN_STATES: List<JoinState> = listOf(
        JoinState("public", JoinStatePermission("", "simplecloud.proxy-essentials.join.full.public")),
        JoinState(
            "maintenance",
            JoinStatePermission(
                "simplecloud.proxy-essentials.join.maintenance",
                "simplecloud.proxy-essentials.join.maintenance"
            ),
            "maintenance"
        )
    )

    val TABLIST_GROUPS: List<TabListGroup> = listOf(
        TabListGroup(
            name = "global",
            layout = listOf(TabList()),
            updateTime = 20L
        )
    )
}