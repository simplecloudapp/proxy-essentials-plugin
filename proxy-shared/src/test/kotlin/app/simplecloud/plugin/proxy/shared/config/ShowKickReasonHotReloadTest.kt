package app.simplecloud.plugin.proxy.shared.config

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShowKickReasonHotReloadTest {

    @Test
    fun `reloads show kick reason after config file changes`() {
        val directory = Files.createTempDirectory("proxy-essentials-hot-reload-")
        val configFile = directory.resolve("config.yml")
        Files.writeString(configFile, configWithShowKickReason(false))

        val yamlConfig = YamlConfig(directory.toString())
        try {
            val reactiveConfig = yamlConfig.load<ProxyEssentialsConfig>("config")
            assertFalse(reactiveConfig.get().showKickReason)

            Files.writeString(configFile, configWithShowKickReason(true))

            val deadline = System.nanoTime() + 5_000_000_000L
            while (!reactiveConfig.get().showKickReason && System.nanoTime() < deadline) {
                Thread.sleep(25)
            }

            assertTrue(reactiveConfig.get().showKickReason)
        } finally {
            yamlConfig.close()
            directory.toFile().deleteRecursively()
        }
    }

    private fun configWithShowKickReason(enabled: Boolean): String {
        return """
            version: '2'
            initial-state: public
            initial-layout: public
            show-kick-reason: $enabled
            joinstates: []
            tablist: []
        """.trimIndent()
    }
}
