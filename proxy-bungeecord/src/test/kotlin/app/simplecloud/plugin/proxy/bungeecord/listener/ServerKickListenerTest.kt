package app.simplecloud.plugin.proxy.bungeecord.listener

import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.config.ServerInfo
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.event.ServerKickEvent
import net.md_5.bungee.event.EventHandler
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class ServerKickListenerTest {

    private var disconnectedReason: BaseComponent? = null
    private val player = playerStub()
    private val kickedFrom = stub(ServerInfo::class.java)
    private val fallback = stub(ServerInfo::class.java)
    private val serverReason = TextComponent("You are banned")

    @Test
    fun `uses the server reason when no fallback is available`() {
        val event = kickEvent(cancelServer = null)

        ServerKickListener { true }.handle(event)

        assertSame(serverReason, disconnectedReason)
    }

    @Test
    fun `does nothing when forwarding is disabled`() {
        val event = kickEvent(cancelServer = null)

        ServerKickListener { false }.handle(event)

        assertNull(disconnectedReason)
    }

    @Test
    fun `leaves a fallback redirect untouched`() {
        val event = kickEvent(cancelServer = fallback).apply { isCancelled = true }

        ServerKickListener { true }.handle(event)

        assertSame(fallback, event.cancelServer)
        assertNull(disconnectedReason)
    }

    @Test
    fun `does nothing when the server did not provide a reason`() {
        val event = kickEvent(cancelServer = null, reason = null)

        ServerKickListener { true }.handle(event)

        assertNull(disconnectedReason)
    }

    @Test
    fun `applies hot-reloaded setting without recreating listener`() {
        var enabled = false
        val listener = ServerKickListener { enabled }
        val event = kickEvent(cancelServer = null)

        listener.handle(event)
        assertNull(disconnectedReason)

        enabled = true
        listener.handle(event)

        assertSame(serverReason, disconnectedReason)
    }

    @Test
    fun `runs after fallback listeners`() {
        val annotation = ServerKickListener::class.java
            .getDeclaredMethod("handle", ServerKickEvent::class.java)
            .getAnnotation(EventHandler::class.java)

        assertEquals(Byte.MAX_VALUE, annotation.priority)
    }

    private fun kickEvent(
        cancelServer: ServerInfo?,
        reason: BaseComponent? = serverReason
    ): ServerKickEvent {
        return ServerKickEvent(
            player,
            kickedFrom,
            reason,
            cancelServer,
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
