package app.simplecloud.plugin.proxy.shared.config.tablis

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class TabList(
    val header: List<String> = listOf(
        "",
        "<color:#0ea5e9>SimpleCloud v3",
        ""
    ),
    val footer: List<String> = listOf(
        "",
        " <color:#ffffff><online_players> players <color:#cbd5e1>are playing on your network ",
        "<color:#64748b>  sɪᴍᴘʟᴇᴄʟᴏᴜᴅ.ᴀᴘᴘ",
        ""
    ),
)
