package app.simplecloud.plugin.proxy.velocity.listener

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.KickedFromServerEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.server.RegisteredServer
import net.kyori.adventure.text.Component
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class ServerKickListenerTest {

    private var disconnectedReason: Component? = null
    private val player = playerStub()
    private val server = stub(RegisteredServer::class.java)
    private val serverReason = Component.text("You are banned")
    private val proxyReason = Component.text("No fallback server available")

    @Test
    fun `uses the server reason instead of the proxy reason`() {
        val event = kickEvent(KickedFromServerEvent.DisconnectPlayer.create(proxyReason))

        ServerKickListener { true }.handle(event)

        val result = event.result as KickedFromServerEvent.DisconnectPlayer
        assertSame(serverReason, result.reasonComponent)
        assertSame(serverReason, disconnectedReason)
    }

    @Test
    fun `keeps the proxy reason when forwarding is disabled`() {
        val originalResult = KickedFromServerEvent.DisconnectPlayer.create(proxyReason)
        val event = kickEvent(originalResult)

        ServerKickListener { false }.handle(event)

        assertSame(originalResult, event.result)
        assertNull(disconnectedReason)
    }

    @Test
    fun `uses the server reason instead of a fallback redirect`() {
        val event = kickEvent(KickedFromServerEvent.RedirectPlayer.create(server))

        ServerKickListener { true }.handle(event)

        val result = event.result as KickedFromServerEvent.DisconnectPlayer
        assertSame(serverReason, result.reasonComponent)
        assertSame(serverReason, disconnectedReason)
    }

    @Test
    fun `keeps the proxy reason when the server did not provide one`() {
        val originalResult = KickedFromServerEvent.DisconnectPlayer.create(proxyReason)
        val event = KickedFromServerEvent(player, server, null, true, originalResult)

        ServerKickListener { true }.handle(event)

        assertSame(originalResult, event.result)
        assertNull(disconnectedReason)
    }

    @Test
    fun `applies hot-reloaded setting without recreating listener`() {
        var enabled = false
        val listener = ServerKickListener { enabled }
        val originalResult = KickedFromServerEvent.RedirectPlayer.create(server)
        val event = kickEvent(originalResult)

        listener.handle(event)
        assertSame(originalResult, event.result)
        assertNull(disconnectedReason)

        enabled = true
        listener.handle(event)

        val result = event.result as KickedFromServerEvent.DisconnectPlayer
        assertSame(serverReason, result.reasonComponent)
        assertSame(serverReason, disconnectedReason)
    }

    @Test
    fun `runs before fallback listeners`() {
        val annotation = ServerKickListener::class.java
            .getDeclaredMethod("handle", KickedFromServerEvent::class.java)
            .getAnnotation(Subscribe::class.java)

        assertEquals(Short.MAX_VALUE, annotation.priority)
    }

    @Test
    fun `keeps the original reason when a later fallback handler disconnects`() {
        val event = kickEvent(KickedFromServerEvent.RedirectPlayer.create(server))

        ServerKickListener { true }.handle(event)
        event.player.disconnect(proxyReason)

        assertSame(serverReason, disconnectedReason)
    }

    private fun kickEvent(result: KickedFromServerEvent.ServerKickResult): KickedFromServerEvent {
        return KickedFromServerEvent(player, server, serverReason, true, result)
    }

    @Suppress("UNCHECKED_CAST")
    private fun playerStub(): Player {
        return Proxy.newProxyInstance(Player::class.java.classLoader, arrayOf(Player::class.java)) { _, method, args ->
            if (method.name == "disconnect") {
                if (disconnectedReason == null) {
                    disconnectedReason = args?.firstOrNull() as? Component
                }
                null
            } else {
                throw UnsupportedOperationException("Unexpected call to ${method.name}")
            }
        } as Player
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> stub(type: Class<T>): T {
        return Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, _ ->
            throw UnsupportedOperationException("Unexpected call to ${method.name}")
        } as T
    }
}
