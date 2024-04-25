package app.simplecloud.plugin.proxy.bungeecord.event

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Event

class ConfigureTagResolversEvent(
    val player: ProxiedPlayer? = null,
    val tagResolvers: MutableList<TagResolver> = mutableListOf()
): Event() {

    fun withTagResolvers(vararg tagResolvers: TagResolver): ConfigureTagResolversEvent {
        this.tagResolvers.addAll(tagResolvers)
        return this
    }

}