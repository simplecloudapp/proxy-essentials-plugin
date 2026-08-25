package app.simplecloud.plugin.proxy.shared.utilities

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import kotlin.math.roundToInt

class ServerIconLoader<T>(
    directory: Path,
    private val createIcon: (BufferedImage) -> T
) {

    private val directory = directory.toAbsolutePath().normalize()
    private val cache = ConcurrentHashMap<Path, CacheEntry<T>>()
    private val warnedInvalidNames = ConcurrentHashMap.newKeySet<String>()

    init {
        Files.createDirectories(this.directory)
    }

    fun get(fileName: String, warn: (String) -> Unit): T? {
        val iconPath = resolveIconPath(fileName, warn) ?: return null
        val snapshot = IconSnapshot(
            path = iconPath,
            lastModified = if (Files.exists(iconPath)) Files.getLastModifiedTime(iconPath).toMillis() else -1,
            length = if (Files.exists(iconPath)) Files.size(iconPath) else -1
        )

        cache[iconPath]?.takeIf { it.snapshot == snapshot }?.let { return it.icon }

        if (!Files.isRegularFile(iconPath)) {
            cache[iconPath] = CacheEntry(snapshot, null)
            warn("Server icon does not exist: ${iconPath.fileName}")
            return null
        }

        return runCatching {
            val image = ImageIO.read(iconPath.toFile()) ?: error("Unsupported image format")
            val favicon = createIcon(image.toFaviconImage())
            cache[iconPath] = CacheEntry(snapshot, favicon)
            favicon
        }.onFailure {
            cache[iconPath] = CacheEntry(snapshot, null)
            warn("Failed to load server icon ${iconPath.fileName}: ${it.message}")
        }.getOrNull()
    }

    private fun resolveIconPath(fileName: String, warn: (String) -> Unit): Path? {
        if (fileName.isBlank()) {
            warnOnce(fileName, warn, "Server icon file name is blank")
            return null
        }

        val iconPath = directory.resolve(fileName).normalize()
        if (!iconPath.startsWith(directory)) {
            warnOnce(fileName, warn, "Server icon path is outside of the server-icons directory: $fileName")
            return null
        }
        return iconPath
    }

    private fun warnOnce(fileName: String, warn: (String) -> Unit, message: String) {
        if (warnedInvalidNames.add(fileName)) warn(message)
    }

    private fun BufferedImage.toFaviconImage(): BufferedImage {
        if (width == FAVICON_SIZE && height == FAVICON_SIZE) return this

        val scale = minOf(FAVICON_SIZE.toDouble() / width, FAVICON_SIZE.toDouble() / height)
        val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
        val targetX = (FAVICON_SIZE - targetWidth) / 2
        val targetY = (FAVICON_SIZE - targetHeight) / 2

        return BufferedImage(FAVICON_SIZE, FAVICON_SIZE, BufferedImage.TYPE_INT_ARGB).also { favicon ->
            val graphics = favicon.createGraphics()
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                graphics.drawImage(this, targetX, targetY, targetWidth, targetHeight, null)
            } finally {
                graphics.dispose()
            }
        }
    }

    private data class IconSnapshot(
        val path: Path,
        val lastModified: Long,
        val length: Long
    )

    private data class CacheEntry<T>(
        val snapshot: IconSnapshot,
        val icon: T?
    )

    private companion object {
        const val FAVICON_SIZE = 64
    }
}