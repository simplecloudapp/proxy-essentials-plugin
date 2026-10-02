package app.simplecloud.plugin.proxy.shared.utilities.text

import app.simplecloud.plugin.api.shared.config.ConfigurationFactory
import app.simplecloud.plugin.proxy.shared.config.MessageConfig
import app.simplecloud.plugin.proxy.shared.layout.LayoutService
import app.simplecloud.plugin.proxy.shared.player.PlayerCountService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MessageFormatter(
    private val messages: ConfigurationFactory<MessageConfig>,
    private val playerCountService: PlayerCountService,
    private val layoutService: LayoutService
) {

    private val miniMessage = MiniMessage.miniMessage()

    fun format(text: String, vararg resolvers: TagResolver): Component =
        messages.get().msg(text, getPlaceholders("unknown", -1), *resolvers)

    fun formatForPlayer(text: String, serverName: String, ping: Long): Component =
        messages.get().msg(text, getPlaceholders(serverName, ping))

    fun formatMotd(line1: String, line2: String): Component =
        MotdMiniMessageFormatter.deserialize(miniMessage, line1, line2, listOf(getPlaceholders("unknown", -1)))

    private fun getPlaceholders(serverName: String, ping: Long): TagResolver {
        val format = messages.get().format
        val onlinePlayers = playerCountService.getOnlinePlayers()
        val slots = layoutService.getLayout(layoutService.getLayoutName(null)).version.slots

        return TagResolver.resolver(
            Placeholder.unparsed("server_name", serverName),
            Placeholder.parsed("ping", getPingColor(ping) + ping),
            Placeholder.unparsed("online_players", onlinePlayers.toString()),
            Placeholder.unparsed("max_players", slots.resolveMaxPlayers(onlinePlayers, playerCountService.getMaxPlayers()).toString()),
            getDateTimeResolver("date", format.date),
            getDateTimeResolver("time", format.time),
            getEnvironmentResolver()
        )
    }

    private fun getPingColor(ping: Long): String {
        val pingColor = messages.get().format.pingColors.filter { ping >= it.ping }.maxByOrNull { it.ping }
        if (pingColor == null) return "<dark_red>"
        return pingColor.color
    }

    private fun getDateTimeResolver(name: String, defaultPattern: String): TagResolver {
        return TagResolver.resolver(name) { arguments, _ ->
            val pattern = if (arguments.hasNext()) arguments.pop().value() else defaultPattern
            Tag.selfClosingInserting(Component.text(LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern))))
        }
    }

    private fun getEnvironmentResolver(): TagResolver {
        return TagResolver.resolver("env") { arguments, _ ->
            val name = arguments.popOr("env name expected").value()
            val default = if (arguments.hasNext()) arguments.pop().value() else ""
            Tag.preProcessParsed(System.getenv(name) ?: default)
        }
    }
}