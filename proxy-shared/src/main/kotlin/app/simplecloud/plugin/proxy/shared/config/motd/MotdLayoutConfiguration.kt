package app.simplecloud.plugin.proxy.shared.config.motd

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.awt.image.BufferedImage

@ConfigSerializable
data class MotdLayoutConfiguration(
    val firstLines: List<String> = listOf("<color:#0ea5e9>A simplecloud.app network"),
    val secondLines: List<String> = listOf("<color:#a3e635>● <color:#1e293b>▍ <color:#94a3b8>ʀᴇᴀᴅ <color:#cbd5e1>ᴅᴏᴄs.sɪᴍᴘʟᴇᴄʟᴏᴜᴅ.ᴀᴘᴘ</color> ᴛᴏ ᴄᴏɴғɪɢᴜʀᴇ</color>"),
    val playerInfo: List<String> = listOf(),
    val versionName: String = "",
    var serverIcon: String = "server-icon.png",
    val maxPlayerDisplayType: MaxPlayerDisplayType? = MaxPlayerDisplayType.REAL,
    val dynamicPlayerRange: Int = 1
)