package app.simplecloud.plugin.proxy.shared.handler

import java.net.InetAddress
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalPingSourceMatcherTest {

    @Test
    fun `loopback addresses are local regardless of interface lookup`() {
        val matcher = LocalPingSourceMatcher { false }

        assertTrue(matcher.isLocal(InetAddress.getLoopbackAddress()))
    }

    @Test
    fun `addresses assigned to another local interface are local`() {
        val localAddress = InetAddress.getByName("192.0.2.10")
        val matcher = LocalPingSourceMatcher { it == localAddress }

        assertTrue(matcher.isLocal(localAddress))
    }

    @Test
    fun `unassigned and missing addresses are not local`() {
        val matcher = LocalPingSourceMatcher { false }

        assertFalse(matcher.isLocal(InetAddress.getByName("192.0.2.20")))
        assertFalse(matcher.isLocal(null))
    }

    @Test
    fun `failed interface lookups do not treat a source as local`() {
        val matcher = LocalPingSourceMatcher { error("lookup failed") }

        assertFalse(matcher.isLocal(InetAddress.getByName("192.0.2.30")))
    }
}
