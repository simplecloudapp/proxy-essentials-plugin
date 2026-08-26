package app.simplecloud.plugin.proxy.bungeecord

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer
import net.md_5.bungee.api.chat.BaseComponent

fun Component.toBaseComponent(): BaseComponent {
    return BungeeComponentSerializer.get().serialize(this)[0]
}
