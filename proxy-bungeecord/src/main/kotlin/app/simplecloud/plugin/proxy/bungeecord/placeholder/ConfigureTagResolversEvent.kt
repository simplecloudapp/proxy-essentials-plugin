package app.simplecloud.plugin.proxy.bungeecord.placeholder

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Event

/**
 * Fired before a message is deserialized so other plugins can contribute their own MiniMessage tags.
 */
class ConfigureTagResolversEvent(
    val player: ProxiedPlayer? = null,
    val tagResolvers: MutableList<TagResolver> = mutableListOf()
) : Event() {

    fun withTagResolvers(vararg tagResolvers: TagResolver): ConfigureTagResolversEvent {
        return withTagResolvers(tagResolvers.toList())
    }

    fun withTagResolvers(tagResolvers: List<TagResolver>): ConfigureTagResolversEvent {
        this.tagResolvers.addAll(tagResolvers)
        return this
    }
}
