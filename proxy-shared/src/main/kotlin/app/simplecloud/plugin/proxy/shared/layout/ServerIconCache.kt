package app.simplecloud.plugin.proxy.shared.layout

import org.slf4j.LoggerFactory
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import kotlin.math.roundToInt

class ServerIconCache(
    directory: Path
) {

    private val logger = LoggerFactory.getLogger(ServerIconCache::class.java)
    private val directory = directory.toAbsolutePath().normalize()
    private val icons = ConcurrentHashMap<Path, Pair<Long, String?>>()

    fun get(fileName: String): String? {
        val file = directory.resolve(fileName).normalize()
        val lastModified = getLastModified(file)
        val cached = icons[file]
        if (cached != null && cached.first == lastModified) return cached.second

        val icon = loadIcon(fileName, file, lastModified)
        icons[file] = lastModified to icon
        return icon
    }

    private fun getLastModified(file: Path): Long {
        if (!file.startsWith(directory) || !Files.isRegularFile(file)) return MISSING
        return Files.getLastModifiedTime(file).toMillis()
    }

    private fun loadIcon(fileName: String, file: Path, lastModified: Long): String? {
        if (lastModified == MISSING) {
            logger.warn("The server icon '$fileName' does not exist in '$directory'")
            return null
        }

        return try {
            val image = ImageIO.read(file.toFile()) ?: error("the image format is not supported")
            val output = ByteArrayOutputStream()
            ImageIO.write(scaleToFavicon(image), "png", output)
            "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray())
        } catch (e: Exception) {
            logger.warn("Could not load the server icon '$fileName': ${e.message}")
            null
        }
    }

    private fun scaleToFavicon(image: BufferedImage): BufferedImage {
        if (image.width == FAVICON_SIZE && image.height == FAVICON_SIZE) return image

        val scale = minOf(FAVICON_SIZE.toDouble() / image.width, FAVICON_SIZE.toDouble() / image.height)
        val width = (image.width * scale).roundToInt().coerceAtLeast(1)
        val height = (image.height * scale).roundToInt().coerceAtLeast(1)

        val favicon = BufferedImage(FAVICON_SIZE, FAVICON_SIZE, BufferedImage.TYPE_INT_ARGB)
        val graphics = favicon.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics.drawImage(image, (FAVICON_SIZE - width) / 2, (FAVICON_SIZE - height) / 2, width, height, null)
        } finally {
            graphics.dispose()
        }
        return favicon
    }

    private companion object {
        const val FAVICON_SIZE = 64
        const val MISSING = -1L
    }
}
