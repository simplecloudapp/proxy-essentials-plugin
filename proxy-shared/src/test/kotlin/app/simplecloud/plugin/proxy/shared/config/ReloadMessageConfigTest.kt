package app.simplecloud.plugin.proxy.shared.config

import app.simplecloud.plugin.proxy.shared.config.message.MessageConfig
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class ReloadMessageConfigTest {

    @Test
    fun `loads customizable reload messages`() {
        val directory = Files.createTempDirectory("proxy-essentials-reload-messages-")
        Files.writeString(
            directory.resolve("messages.yml"),
            """
                version: '2'
                variables:
                    prefix: '[Proxy] '
                command:
                    reload:
                        start: '<prefix>Starting reload'
                        success: '<prefix>Reload complete'
                        failure: '<prefix>Reload failed: <error>'
            """.trimIndent()
        )

        val yamlConfig = YamlConfig(directory.toString())
        try {
            val messages = yamlConfig.load<MessageConfig>("messages").get()

            assertEquals("[Proxy] Starting reload", messages.resolve(messages.command.reload.start))
            assertEquals("[Proxy] Reload complete", messages.resolve(messages.command.reload.success))
            assertEquals(
                "[Proxy] Reload failed: broken",
                messages.resolve(messages.command.reload.failure).replace("<error>", "broken")
            )
        } finally {
            yamlConfig.close()
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `adds reload messages to existing message configs`() {
        val directory = Files.createTempDirectory("proxy-essentials-reload-message-migration-")
        val messagesFile = directory.resolve("messages.yml")
        Files.writeString(
            messagesFile,
            """
                version: '1'
                variables:
                    prefix: '<green>Custom </green>'
                kick:
                    network-full: 'Keep this custom message'
                command: {}
            """.trimIndent()
        )

        try {
            OldConfigMigrator.migrate(directory)

            val yamlConfig = YamlConfig(directory.toString())
            try {
                val messages = yamlConfig.load<MessageConfig>("messages").get()
                assertEquals("2", messages.version)
                assertEquals("<green>Custom </green>", messages.variables.prefix)
                assertEquals("Keep this custom message", messages.kick.networkFull)
                assertEquals(
                    MessageConfig().command.reload.failure,
                    messages.command.reload.failure
                )
            } finally {
                yamlConfig.close()
            }
        } finally {
            directory.toFile().deleteRecursively()
        }
    }
}
