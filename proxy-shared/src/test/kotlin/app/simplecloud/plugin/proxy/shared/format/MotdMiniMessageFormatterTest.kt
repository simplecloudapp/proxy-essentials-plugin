package app.simplecloud.plugin.proxy.shared.format

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ObjectComponent
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MotdMiniMessageFormatterTest {

    private val miniMessage = MiniMessage.miniMessage()
    private val pixel = "\u200c"

    @Test
    fun `centers both motd lines independently`() {
        val component = MotdMiniMessageFormatter.deserialize(
            miniMessage,
            "<center>iiii</center>",
            "<center>WW</center>"
        )

        val lines = plainText(component).split('\n')
        assertEquals(" ".repeat(28) + pixel.repeat(2) + "iiii", lines[0])
        assertEquals(" ".repeat(28) + "WW", lines[1])
    }

    @Test
    fun `accounts for bold glyph width`() {
        val component = MotdMiniMessageFormatter.deserialize(
            miniMessage,
            "<center><bold>iiii</bold></center>",
            ""
        )

        assertEquals(" ".repeat(28) + "iiii\n", plainText(component))
    }

    @Test
    fun `accounts for inherited bold glyph width`() {
        val component = MotdMiniMessageFormatter.deserialize(
            miniMessage,
            "<bold><center>iiii</center></bold>",
            ""
        )

        assertEquals(" ".repeat(28) + "iiii\n", plainText(component))
    }

    @Test
    fun `measures resolved component placeholders`() {
        val value = Component.text("iiii").decorate(TextDecoration.BOLD)
        val component = MotdMiniMessageFormatter.deserialize(
            miniMessage,
            "<center><value></center>",
            "",
            listOf(Placeholder.component("value", value))
        )

        assertEquals(" ".repeat(28) + "iiii\n", plainText(component))
    }

    @Test
    fun `uses metrics for built in fonts and symbols`() {
        val component = MotdMiniMessageFormatter.deserialize(
            miniMessage,
            "<center><font:minecraft:alt>cccc</font></center>",
            "<center><font:minecraft:uniform>WW</font>🔥</center>"
        )

        val lines = plainText(component).split('\n')
        assertEquals(" ".repeat(28) + "cccc", lines[0])
        assertEquals(" ".repeat(27) + pixel.repeat(2) + "WW🔥", lines[1])
    }

    @Test
    fun `uses fractional unihex metrics for unicode symbols`() {
        val component = MotdMiniMessageFormatter.deserialize(
            miniMessage,
            "<center><gradient:#364153:#99A1AF>⬢ ⬢ ⬢ ⬢ ⬢</gradient>  " +
                "<bold><gradient:#5ED2EA:#3A89E3>ʙʟᴏᴄᴋʙᴏᴜɴᴅ.ɢɢ</bold>  " +
                "<gradient:#99A1AF:#364153>⬢ ⬢ ⬢ ⬢ ⬢</gradient>",
            ""
        )

        assertEquals(
            " ".repeat(4) + pixel.repeat(2) +
                "⬢ ⬢ ⬢ ⬢ ⬢  ʙʟᴏᴄᴋʙᴏᴜɴᴅ.ɢɢ  ⬢ ⬢ ⬢ ⬢ ⬢\n",
            plainText(component)
        )
    }

    @Test
    fun `accounts for sprite object components`() {
        val component = MotdMiniMessageFormatter.deserialize(
            miniMessage,
            "<center><sprite:minecraft:block/stone></center>",
            ""
        )

        assertEquals(" ".repeat(28) + pixel.repeat(2) + "\n", plainText(component))
        assertIs<ObjectComponent>(component.descendants().single { it is ObjectComponent })
    }

    @Test
    fun `centers relative to the complete server list row`() {
        val text = "A simplecloud.app network"
        val component = MotdMiniMessageFormatter.deserialize(
            miniMessage,
            "<center><color:#0ea5e9>$text",
            " "
        )

        val firstLine = plainText(component).substringBefore('\n')
        val padding = firstLine.takeWhile { it == ' ' || it == pixel.single() }
        val paddingWidth = padding.count { it == ' ' } * 4f + padding.count { it == pixel.single() }
        val textWidth = MinecraftTextWidth.width(miniMessage.deserialize(text))
        val visualCenter = 35f + paddingWidth + (textWidth / 2f)

        assertTrue(abs(visualCenter - (305f / 2f)) <= 0.5f)
        assertEquals(" ", plainText(component).substringAfter('\n'))
    }

    @Test
    fun `leaves untagged lines unchanged`() {
        val component = MotdMiniMessageFormatter.deserialize(miniMessage, "hello", "world")

        assertEquals("hello\nworld", plainText(component))
    }

    private fun plainText(component: Component): String {
        val content = (component as? TextComponent)?.content().orEmpty()
        return content + component.children().joinToString(separator = "") { plainText(it) }
    }

    private fun Component.descendants(): List<Component> =
        children() + children().flatMap { it.descendants() }
}
