package app.simplecloud.plugin.proxy.shared.resolver

import app.simplecloud.plugin.proxy.shared.config.motd.MaxPlayerDisplayType
import app.simplecloud.plugin.proxy.shared.config.motd.MotdLayoutConfiguration
import app.simplecloud.plugin.proxy.shared.config.placeholder.PingColor
import net.kyori.adventure.text.minimessage.Context
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue
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
        val pingColor = pingColors.firstOrNull { it.ping >= ping }?.color ?: "<dark_red>"
        return Placeholder.parsed(TagResolverNames.PING, "$pingColor$ping")
    }

    fun getDateTagResolver(): TagResolver {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        return Formatter.date(TagResolverNames.DATE, now)
    }

    fun getMaxPlayersTagResolver(
        onlinePlayers: Int,
        realMaxPlayers: Int,
        motdConfiguration: MotdLayoutConfiguration
    ): TagResolver {
        val maxPlayers = when (motdConfiguration.maxPlayerDisplayType) {
            MaxPlayerDisplayType.REAL -> realMaxPlayers
            MaxPlayerDisplayType.DYNAMIC -> onlinePlayers + motdConfiguration.dynamicPlayerRange
            null -> realMaxPlayers
        }
        return Placeholder.unparsed(TagResolverNames.MAX_PLAYERS, maxPlayers.toString())
    }

    fun getOnlinePlayersTagResolver(onlinePlayers: Int): TagResolver {
        return Placeholder.unparsed(TagResolverNames.ONLINE_PLAYERS, onlinePlayers.toString())
    }

    fun getEnvTagResolver(): TagResolver {
        return TagResolver.resolver(
            TagResolverNames.ENV
        ) { args: ArgumentQueue, context: Context? ->
            val envName = args.popOr("env name expected").value()
            val envDefault = args.peek()?.value()?: ""
            val env = System.getenv(envName) ?: envDefault

            Tag.preProcessParsed(env)
        }

    }

}