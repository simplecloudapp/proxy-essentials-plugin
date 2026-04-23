package app.simplecloud.plugin.proxy.shared.config.tablis

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class TabList(
    val header: String = "<br><color:#0ea5e9>SimpleCloud v3<br>",
    val footer: String = "<br> <color:#ffffff><online_players> players <color:#cbd5e1>are playing on your network <br> <color:#64748b>  sɪᴍᴘʟᴇᴄʟᴏᴜᴅ.ᴀᴘᴘ<br>",
)
