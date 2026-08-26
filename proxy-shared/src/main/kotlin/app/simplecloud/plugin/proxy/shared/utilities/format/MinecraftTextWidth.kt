package app.simplecloud.plugin.proxy.shared.utilities.format

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.KeybindComponent
import net.kyori.adventure.text.ObjectComponent
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration

object MinecraftTextWidth {

    const val ZERO_WIDTH_NON_JOINER = 0x200c

    private const val RESOURCE_DIRECTORY = "/app/simplecloud/plugin/proxy/shared/format"
    private const val MISSING_GLYPH_WIDTH = 6f
    private const val OBJECT_WIDTH = 8f
    private const val SPACE_WIDTH = 4f
    private const val BITMAP_BOLD_OFFSET = 1f
    private const val UNIHEX_BOLD_OFFSET = 0.5f
    private const val UNICODE_CODE_POINT_COUNT = 0x110000

    private val defaultFont = Key.key("minecraft", "default")
    private val altFont = Key.key("minecraft", "alt")
    private val uniformFont = Key.key("minecraft", "uniform")

    private val defaultWidths by lazy { loadWidths("default-font-widths.txt") }
    private val altWidths by lazy { loadWidths("alt-font-widths.txt") }
    private val unifontAdvances by lazy { loadAdvances("unifont-advances.bin") }

    fun width(component: Component, inheritedStyle: Style = Style.empty()): Float {
        val style = component.style().merge(inheritedStyle, Style.Merge.Strategy.IF_ABSENT_ON_TARGET)
        val ownWidth = when (component) {
            is TextComponent -> textWidth(component.content(), style)
            is ObjectComponent -> OBJECT_WIDTH + objectBoldOffset(style)
            is TranslatableComponent -> textWidth(component.fallback() ?: component.key(), style)
            is KeybindComponent -> textWidth(component.keybind(), style)
            else -> 0f
        }

        return ownWidth + component.children().sumOf { width(it, style).toDouble() }.toFloat()
    }

    private fun objectBoldOffset(style: Style): Float {
        if (!style.hasDecoration(TextDecoration.BOLD)) return 0f
        return BITMAP_BOLD_OFFSET
    }

    private fun textWidth(text: String, style: Style): Float {
        val bold = style.hasDecoration(TextDecoration.BOLD)
        return text.codePoints().toArray()
            .sumOf { glyphWidth(it, style.font(), bold).toDouble() }
            .toFloat()
    }

    private fun glyphWidth(codePoint: Int, font: Key?, bold: Boolean): Float {
        val metrics = metricsOf(codePoint, font) ?: GlyphMetrics(MISSING_GLYPH_WIDTH, BITMAP_BOLD_OFFSET)
        if (!bold) return metrics.advance
        return metrics.advance + metrics.boldOffset
    }

    private fun metricsOf(codePoint: Int, font: Key?): GlyphMetrics? {
        return when {
            codePoint == ' '.code -> GlyphMetrics(SPACE_WIDTH, BITMAP_BOLD_OFFSET)
            codePoint == ZERO_WIDTH_NON_JOINER -> GlyphMetrics(0f, BITMAP_BOLD_OFFSET)
            font == null || font == defaultFont -> bitmapMetrics(defaultWidths, codePoint) ?: unihexMetrics(codePoint)
            font == altFont -> bitmapMetrics(altWidths, codePoint)
            font == uniformFont -> unihexMetrics(codePoint)
            else -> null
        }
    }

    private fun bitmapMetrics(widths: Map<Int, Float>, codePoint: Int): GlyphMetrics? {
        val advance = widths[codePoint] ?: return null
        return GlyphMetrics(advance, BITMAP_BOLD_OFFSET)
    }

    private fun unihexMetrics(codePoint: Int): GlyphMetrics? {
        if (codePoint !in unifontAdvances.indices) return null

        val encodedAdvance = unifontAdvances[codePoint].toInt() and 0xff
        if (encodedAdvance == 0) return null

        return GlyphMetrics(encodedAdvance / 2f, UNIHEX_BOLD_OFFSET)
    }

    private fun loadWidths(resourceName: String): Map<Int, Float> {
        return openResource(resourceName).bufferedReader().useLines { lines ->
            lines
                .filterNot { it.isBlank() || it.startsWith('#') }
                .associate { line ->
                    val (codePoint, width) = line.split('=', limit = 2)
                    codePoint.toInt(16) to width.toFloat()
                }
        }
    }

    private fun loadAdvances(resourceName: String): ByteArray {
        val advances = openResource(resourceName).use { it.readBytes() }

        check(advances.size == UNICODE_CODE_POINT_COUNT) {
            "Invalid font metrics resource $resourceName: expected $UNICODE_CODE_POINT_COUNT bytes, got ${advances.size}"
        }
        return advances
    }

    private fun openResource(resourceName: String) =
        checkNotNull(javaClass.getResourceAsStream("$RESOURCE_DIRECTORY/$resourceName")) {
            "Missing font metrics resource $RESOURCE_DIRECTORY/$resourceName"
        }

    private data class GlyphMetrics(val advance: Float, val boldOffset: Float)
}