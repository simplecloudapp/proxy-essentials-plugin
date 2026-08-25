package app.simplecloud.plugin.proxy.shared.format

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.KeybindComponent
import net.kyori.adventure.text.ObjectComponent
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import kotlin.math.roundToInt

object MotdMiniMessageFormatter {

    private const val CENTER_TAG = "center"
    private const val CENTER_MARKER = "simplecloud:motd_center"
    private const val SERVER_LIST_ROW_WIDTH = 305f
    // MOTD text starts after the 32-pixel icon and its 3-pixel gap.
    private const val MOTD_START_OFFSET = 35f
    // Center relative to the complete row, not just the area remaining after the icon.
    private const val MOTD_CENTER_WIDTH = SERVER_LIST_ROW_WIDTH - (MOTD_START_OFFSET * 2)
    private const val SPACE_WIDTH = 4
    private const val PIXEL_PADDING_CHARACTER = '\u200c'
    private val defaultFont = Key.key("minecraft", "default")
    private val mergesWithoutInsertion = Style.Merge.all().minus(Style.Merge.INSERTION)

    private val centerTagResolver = TagResolver.resolver(CENTER_TAG) { _, _ ->
        Tag.inserting(Component.empty().insertion(CENTER_MARKER))
    }

    fun deserialize(
        miniMessage: MiniMessage,
        line1: String,
        line2: String,
        tagResolvers: List<TagResolver> = emptyList()
    ): Component {
        val resolvers = arrayOf(centerTagResolver, *tagResolvers.toTypedArray())

        // Treat both configured lines independently so an omitted closing tag cannot leak into
        // the other line. MiniMessage closes still-open tags at the end of each input.
        return Component.empty()
            .append(applyCentering(miniMessage.deserialize(line1, *resolvers)))
            .append(Component.newline())
            .append(applyCentering(miniMessage.deserialize(line2, *resolvers)))
    }

    /** Drops the [CENTER_MARKER] insertion again once the marked component has been measured. */
    private fun clearInsertion(style: Style): Style {
        return Style.empty().merge(style, Style.Merge.Strategy.ALWAYS, mergesWithoutInsertion)
    }

    private fun applyCentering(component: Component, inheritedStyle: Style = Style.empty()): Component {
        val style = component.style().merge(inheritedStyle, Style.Merge.Strategy.IF_ABSENT_ON_TARGET)
        var processed = component.children(
            component.children().map { applyCentering(it, style) }
        )

        // Resolve on the finished tree so styles outside <center> and inserted components are measurable.
        if (processed.insertion() != CENTER_MARKER) return processed

        processed = processed.style(clearInsertion(processed.style()))
        val contentWidth = MinecraftTextWidth.width(processed, inheritedStyle)
        val paddingWidth = ((MOTD_CENTER_WIDTH - contentWidth) / 2f)
            .roundToInt()
            .coerceAtLeast(0)

        if (paddingWidth == 0) return processed

        val padding = Component.text()
            .append(
                Component.text(" ".repeat(paddingWidth / SPACE_WIDTH))
                    .font(defaultFont)
                    .decoration(TextDecoration.BOLD, false)
            )
            // U+200C has zero advance in Minecraft's space provider. Bold contributes its
            // one-pixel bold offset while the glyph itself remains invisible.
            .append(
                Component.text(PIXEL_PADDING_CHARACTER.toString().repeat(paddingWidth % SPACE_WIDTH))
                    .font(defaultFont)
                    .decoration(TextDecoration.BOLD, true)
            )
            .build()

        return Component.empty().append(padding).append(processed)
    }
}

internal object MinecraftTextWidth {

    private const val MISSING_GLYPH_WIDTH = 6f
    private const val OBJECT_WIDTH = 8f
    private const val BITMAP_BOLD_OFFSET = 1f
    private const val UNIHEX_BOLD_OFFSET = 0.5f
    private const val UNICODE_CODE_POINT_COUNT = 0x110000
    private val defaultFont = Key.key("minecraft", "default")
    private val mergesWithoutInsertion = Style.Merge.all().minus(Style.Merge.INSERTION)
    private val altFont = Key.key("minecraft", "alt")
    private val uniformFont = Key.key("minecraft", "uniform")

    private val defaultWidths by lazy { loadWidths("default-font-widths.txt") }
    private val altWidths by lazy { loadWidths("alt-font-widths.txt") }
    private val unifontAdvances by lazy { loadAdvances("unifont-advances.bin") }

    fun width(component: Component, inheritedStyle: Style = Style.empty()): Float =
        calculateWidth(component, inheritedStyle)

    private fun calculateWidth(component: Component, inheritedStyle: Style): Float {
        val style = component.style().merge(inheritedStyle, Style.Merge.Strategy.IF_ABSENT_ON_TARGET)
        val bold = style.hasDecoration(TextDecoration.BOLD)

        val ownWidth = when (component) {
            is TextComponent -> component.content().codePoints().toArray().sumOf {
                glyphWidth(it, style.font(), bold).toDouble()
            }.toFloat()

            is ObjectComponent -> OBJECT_WIDTH + if (bold) 1f else 0f
            is TranslatableComponent -> textWidth(component.fallback() ?: component.key(), style)
            is KeybindComponent -> textWidth(component.keybind(), style)
            else -> 0f
        }

        return ownWidth + component.children().sumOf { calculateWidth(it, style).toDouble() }.toFloat()
    }

    private fun textWidth(text: String, style: Style): Float {
        val bold = style.hasDecoration(TextDecoration.BOLD)
        return text.codePoints().toArray().sumOf {
            glyphWidth(it, style.font(), bold).toDouble()
        }.toFloat()
    }

    private fun glyphWidth(codePoint: Int, font: Key?, bold: Boolean): Float {
        val metrics = when {
            codePoint == ' '.code -> GlyphMetrics(4f, BITMAP_BOLD_OFFSET)
            codePoint == 0x200c -> GlyphMetrics(0f, BITMAP_BOLD_OFFSET)
            font == null || font == defaultFont ->
                defaultWidths[codePoint]?.let { GlyphMetrics(it, BITMAP_BOLD_OFFSET) }
                    ?: unifontWidth(codePoint)?.let { GlyphMetrics(it, UNIHEX_BOLD_OFFSET) }

            font == altFont -> altWidths[codePoint]?.let { GlyphMetrics(it, BITMAP_BOLD_OFFSET) }
            font == uniformFont -> unifontWidth(codePoint)?.let { GlyphMetrics(it, UNIHEX_BOLD_OFFSET) }
            else -> null
        } ?: GlyphMetrics(MISSING_GLYPH_WIDTH, BITMAP_BOLD_OFFSET)

        return metrics.advance + if (bold) metrics.boldOffset else 0f
    }

    private fun unifontWidth(codePoint: Int): Float? {
        if (codePoint !in unifontAdvances.indices) return null

        val encodedAdvance = unifontAdvances[codePoint].toInt() and 0xff
        return encodedAdvance.takeIf { it != 0 }?.div(2f)
    }

    private fun loadWidths(resourceName: String): Map<Int, Float> {
        val resourcePath = "/app/simplecloud/plugin/proxy/shared/format/$resourceName"
        val stream = checkNotNull(javaClass.getResourceAsStream(resourcePath)) {
            "Missing font metrics resource $resourcePath"
        }

        return stream.bufferedReader().useLines { lines ->
            lines
                .filterNot { it.isBlank() || it.startsWith('#') }
                .associate { line ->
                    val (codePoint, width) = line.split('=', limit = 2)
                    codePoint.toInt(16) to width.toFloat()
                }
        }
    }

    private fun loadAdvances(resourceName: String): ByteArray {
        val resourcePath = "/app/simplecloud/plugin/proxy/shared/format/$resourceName"
        val advances = checkNotNull(javaClass.getResourceAsStream(resourcePath)) {
            "Missing font metrics resource $resourcePath"
        }.use { it.readBytes() }

        check(advances.size == UNICODE_CODE_POINT_COUNT) {
            "Invalid font metrics resource $resourcePath: expected $UNICODE_CODE_POINT_COUNT bytes, got ${advances.size}"
        }
        return advances
    }

    private data class GlyphMetrics(val advance: Float, val boldOffset: Float)
}
