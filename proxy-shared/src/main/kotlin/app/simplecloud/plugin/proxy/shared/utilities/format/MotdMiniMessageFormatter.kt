package app.simplecloud.plugin.proxy.shared.utilities.format

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import kotlin.collections.minus
import kotlin.math.roundToInt

object MotdMiniMessageFormatter {

    private const val CENTER_TAG = "center"
    private const val CENTER_MARKER = "simplecloud:motd_center"
    private const val SERVER_LIST_ROW_WIDTH = 305f
    private const val MOTD_START_OFFSET = 35f
    private const val MOTD_CENTER_WIDTH = SERVER_LIST_ROW_WIDTH - (MOTD_START_OFFSET * 2)
    private const val SPACE_WIDTH = 4

    private val pixelPaddingCharacter = Character.toString(MinecraftTextWidth.ZERO_WIDTH_NON_JOINER)
    private val defaultFont = Key.key("minecraft", "default")
    private val mergesWithoutInsertion = Style.Merge.all().minus(Style.Merge.INSERTION)
    private val centerTagResolver = TagResolver.resolver(CENTER_TAG) { _, _ -> Tag.inserting(Component.empty().insertion(CENTER_MARKER)) }

    fun deserialize(
        miniMessage: MiniMessage,
        line1: String,
        line2: String,
        tagResolvers: List<TagResolver> = emptyList()
    ): Component {
        val resolvers = arrayOf(centerTagResolver, *tagResolvers.toTypedArray())
        return Component.empty()
            .append(applyCentering(miniMessage.deserialize(line1, *resolvers)))
            .append(Component.newline())
            .append(applyCentering(miniMessage.deserialize(line2, *resolvers)))
    }

    private fun applyCentering(component: Component, inheritedStyle: Style = Style.empty()): Component {
        val style = component.style().merge(inheritedStyle, Style.Merge.Strategy.IF_ABSENT_ON_TARGET)
        val processed = component.children(component.children().map { applyCentering(it, style) })
        if (processed.insertion() != CENTER_MARKER) return processed

        val centered = processed.style(clearInsertion(processed.style()))
        val paddingWidth = paddingWidthFor(centered, inheritedStyle)
        if (paddingWidth == 0) return centered

        return Component.empty().append(createPadding(paddingWidth)).append(centered)
    }

    private fun paddingWidthFor(component: Component, inheritedStyle: Style): Int {
        val contentWidth = MinecraftTextWidth.width(component, inheritedStyle)
        return ((MOTD_CENTER_WIDTH - contentWidth) / 2f)
            .roundToInt()
            .coerceAtLeast(0)
    }

    private fun createPadding(paddingWidth: Int): Component {
        return Component.text()
            .append(Component.text(" ".repeat(paddingWidth / SPACE_WIDTH)).font(defaultFont).decoration(TextDecoration.BOLD, false))
            .append(Component.text(pixelPaddingCharacter.repeat(paddingWidth % SPACE_WIDTH)).font(defaultFont).decoration(TextDecoration.BOLD, true))
            .build()
    }

    private fun clearInsertion(style: Style): Style {
        return Style.empty().merge(style, Style.Merge.Strategy.ALWAYS, mergesWithoutInsertion)
    }
}