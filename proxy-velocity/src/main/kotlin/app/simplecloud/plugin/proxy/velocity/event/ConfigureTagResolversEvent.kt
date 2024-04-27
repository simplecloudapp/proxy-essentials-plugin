package app.simplecloud.plugin.proxy.velocity.event

import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

class ConfigureTagResolversEvent(
    val player: Player? = null,
    val tagResolvers: MutableList<TagResolver> = mutableListOf()
) {

    fun withTagResolvers(vararg tagResolvers: TagResolver): ConfigureTagResolversEvent {
        return this.withTagResolvers(*tagResolvers)
    }

    fun withTagResolvers(tagResolvers: List<TagResolver>): ConfigureTagResolversEvent {
        this.tagResolvers.addAll(tagResolvers)
        return this
    }

}