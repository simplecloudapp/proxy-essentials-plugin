package app.simplecloud.plugin.proxy.shared.config.tablis

data class TabList(
    val header: List<String> = listOf(
        "<dark_gray>                                                                        <dark_gray>",
        "<aqua>SimpleCloud <dark_gray>» <gray>Simplify <white><italic>your</italic> <gray>network",
        "<dark_aqua>Online <dark_gray>» <gray>%ONLINE_PLAYERS%<dark_gray>/<gray>%MAX_PLAYERS% <dark_gray>┃ <dark_aqua>Server <dark_gray>» <gray>%SERVICE_NAME%",
        "<dark_gray>"
    ),
    val footer: List<String> = listOf(
        "<dark_gray>",
        "<dark_aqua>Twitter <dark_gray>» <gray>@theSimpleCloud",
        "<dark_aqua>Discord <dark_gray>» <gray>discord.simplecloud.app",
        "<dark_gray>"
    ),
)
