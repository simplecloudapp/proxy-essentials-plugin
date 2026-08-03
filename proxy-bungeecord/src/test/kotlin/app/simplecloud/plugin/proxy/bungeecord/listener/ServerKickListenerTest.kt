package app.simplecloud.plugin.proxy.bungeecord.listener

import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.config.ServerInfo
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.event.ServerKickEvent
import net.md_5.bungee.event.EventHandler
import net.md_5.bungee.event.EventPriority
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ServerKickListenerTest {

    private var disconnectedReason: BaseComponent? = null
    private val player = playerStub()
    private val kickedFrom = stub(ServerInfo::class.java)
    private val fallback = stub(ServerInfo::class.java)
    private val serverReason = TextComponent("You are banned")
    private val fallbackReason = TextComponent("No fallback server available")

    @Test
    fun `uses the server reason instead of a fallback redirect`() {
        val event = kickEvent().apply { isCancelled = true }

        ServerKickListener { true }.handle(event)

        assertFalse(event.isCancelled)
        assertNull(event.cancelServer)
        assertSame(serverReason, event.reason)
        assertSame(serverReason, disconnectedReason)
    }

    @Test
    fun `keeps proxy fallback handling when forwarding is disabled`() {
        val event = kickEvent().apply { isCancelled = true }

        ServerKickListener { false }.handle(event)

        assertTrue(event.isCancelled)
        assertSame(fallback, event.cancelServer)
        assertSame(serverReason, event.reason)
        assertNull(disconnectedReason)
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
        assertNull(disconnectedReason)
    }

    @Test
    fun `applies hot-reloaded setting without recreating listener`() {
        var enabled = false
        val listener = ServerKickListener { enabled }
        val event = kickEvent().apply { isCancelled = true }

        listener.handle(event)
        assertTrue(event.isCancelled)
        assertSame(fallback, event.cancelServer)
        assertNull(disconnectedReason)

        enabled = true
        listener.handle(event)

        assertFalse(event.isCancelled)
        assertNull(event.cancelServer)
        assertSame(serverReason, event.reason)
        assertSame(serverReason, disconnectedReason)
    }

    @Test
    fun `runs before fallback listeners`() {
        val annotation = ServerKickListener::class.java
            .getDeclaredMethod("handle", ServerKickEvent::class.java)
            .getAnnotation(EventHandler::class.java)

        assertEquals(EventPriority.LOWEST, annotation.priority)
    }

    @Test
    fun `keeps the original reason when a later fallback handler disconnects`() {
        val event = kickEvent().apply { isCancelled = true }

        ServerKickListener { true }.handle(event)
        event.player.disconnect(fallbackReason)

        assertSame(serverReason, disconnectedReason)
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
    private fun playerStub(): ProxiedPlayer {
        return Proxy.newProxyInstance(
            ProxiedPlayer::class.java.classLoader,
            arrayOf(ProxiedPlayer::class.java)
        ) { _, method, args ->
            if (method.name == "disconnect") {
                if (disconnectedReason == null) {
                    disconnectedReason = args?.firstOrNull() as? BaseComponent
                }
                null
            } else {
                throw UnsupportedOperationException("Unexpected call to ${method.name}")
            }
        } as ProxiedPlayer
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> stub(type: Class<T>): T {
        return Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, _ ->
            throw UnsupportedOperationException("Unexpected call to ${method.name}")
        } as T
    }
}
