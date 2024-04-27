package app.simplecloud.plugin.proxy.shared.config.tablis

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class TabList(
    val header: List<String> = listOf(
        "<dark_gray>                                                                        <dark_gray>",
        "<aqua>SimpleCloud <dark_gray>» <gray>Simplify <white><italic>your</italic> <gray>network",
        "<dark_aqua>Online <dark_gray>» <gray><online_players><dark_gray>/<gray><max_players> <dark_gray>┃ <dark_aqua>Server <dark_gray>» <gray>%SERVICE_NAME%",
        "<dark_gray>"
    ),
    val footer: List<String> = listOf(
        "<dark_gray>",
        "<dark_aqua>Twitter <dark_gray>» <gray>@theSimpleCloud",
        "<dark_aqua>Discord <dark_gray>» <gray>discord.simplecloud.app",
        "<dark_gray>"
    ),
)
