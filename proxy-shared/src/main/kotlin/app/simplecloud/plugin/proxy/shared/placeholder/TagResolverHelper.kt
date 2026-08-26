package app.simplecloud.plugin.proxy.shared.placeholder

import app.simplecloud.plugin.proxy.shared.config.MotdLayoutConfiguration
import app.simplecloud.plugin.proxy.shared.config.PingColor
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import java.time.LocalDateTime
import java.time.ZoneId

object TagResolverHelper {

    fun getDefaultTagResolvers(
        serverName: String,
        ping: Long,
        pingColors: List<PingColor>,
        onlinePlayers: Int,
        realMaxPlayers: Int,
        motdConfiguration: MotdLayoutConfiguration
    ): List<TagResolver> {
        return listOf(
            Placeholder.unparsed(TagResolverNames.SERVER_NAME, serverName),
            getPingTagResolver(ping, pingColors),
            getDateTagResolver(),
            getOnlinePlayersTagResolver(onlinePlayers),
            getMaxPlayersTagResolver(onlinePlayers, realMaxPlayers, motdConfiguration),
            getEnvTagResolver()
        )
    }

    fun getPingTagResolver(ping: Long, pingColors: List<PingColor>): TagResolver {
        val pingColor = pingColors
            .filter { ping >= it.ping }
            .maxByOrNull { it.ping }
            ?.color
            ?: "<dark_red>"

        return Placeholder.parsed(TagResolverNames.PING, "$pingColor$ping")
    }

    fun getDateTagResolver(): TagResolver {
        return Formatter.date(TagResolverNames.DATE, LocalDateTime.now(ZoneId.systemDefault()))
    }

    fun getOnlinePlayersTagResolver(onlinePlayers: Int): TagResolver {
        return Placeholder.unparsed(TagResolverNames.ONLINE_PLAYERS, onlinePlayers.toString())
    }

    fun getMaxPlayersTagResolver(
        onlinePlayers: Int,
        realMaxPlayers: Int,
        motdConfiguration: MotdLayoutConfiguration
    ): TagResolver {
        val maxPlayers = motdConfiguration.version.slots.resolveMaxPlayers(onlinePlayers, realMaxPlayers)
        return Placeholder.unparsed(TagResolverNames.MAX_PLAYERS, maxPlayers.toString())
    }

    fun getEnvTagResolver(): TagResolver {
        return TagResolver.resolver(TagResolverNames.ENV) { arguments, _ ->
            val envName = arguments.popOr("env name expected").value()
            val envDefault = arguments.peek()?.value() ?: ""

            Tag.preProcessParsed(System.getenv(envName) ?: envDefault)
        }
    }
}
