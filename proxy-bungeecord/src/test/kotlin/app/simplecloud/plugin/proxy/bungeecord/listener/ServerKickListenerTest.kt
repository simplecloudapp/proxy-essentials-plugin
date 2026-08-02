package app.simplecloud.plugin.proxy.bungeecord.listener

import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.config.ServerInfo
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.event.ServerKickEvent
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ServerKickListenerTest {

    private val player = stub(ProxiedPlayer::class.java)
    private val kickedFrom = stub(ServerInfo::class.java)
    private val fallback = stub(ServerInfo::class.java)
    private val serverReason = TextComponent("You are banned")

    @Test
    fun `uses the server reason instead of a fallback redirect`() {
        val event = kickEvent().apply { isCancelled = true }

        ServerKickListener { true }.handle(event)

        assertFalse(event.isCancelled)
        assertNull(event.cancelServer)
        assertSame(serverReason, event.reason)
    }

    @Test
    fun `keeps proxy fallback handling when forwarding is disabled`() {
        val event = kickEvent().apply { isCancelled = true }

        ServerKickListener { false }.handle(event)

        assertTrue(event.isCancelled)
        assertSame(fallback, event.cancelServer)
        assertSame(serverReason, event.reason)
    }

    @Test
    fun `keeps proxy fallback handling when the server did not provide a reason`() {
        val event = kickEvent().apply {
            isCancelled = true
            reason = null
        }

        ServerKickListener { true }.handle(event)

        assertTrue(event.isCancelled)
        assertSame(fallback, event.cancelServer)
    }

    @Test
    fun `applies hot-reloaded setting without recreating listener`() {
        var enabled = false
        val listener = ServerKickListener { enabled }
        val event = kickEvent().apply { isCancelled = true }

        listener.handle(event)
        assertTrue(event.isCancelled)
        assertSame(fallback, event.cancelServer)

        enabled = true
        listener.handle(event)

        assertFalse(event.isCancelled)
        assertNull(event.cancelServer)
        assertSame(serverReason, event.reason)
    }

    private fun kickEvent(): ServerKickEvent {
        return ServerKickEvent(
            player,
            kickedFrom,
            serverReason,
            fallback,
            ServerKickEvent.State.CONNECTED
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> stub(type: Class<T>): T {
        return Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, _ ->
            throw UnsupportedOperationException("Unexpected call to ${method.name}")
        } as T
    }
}
