package app.simplecloud.plugin.proxy.shared.handler

import java.net.InetAddress
import java.net.NetworkInterface

class LocalPingSourceMatcher(
    private val isLocalInterfaceAddress: (InetAddress) -> Boolean = {
        NetworkInterface.getByInetAddress(it) != null
    }
) {
    fun isLocal(address: InetAddress?): Boolean {
        if (address == null) {
            return false
        }

        if (address.isLoopbackAddress) {
            return true
        }

        return runCatching { isLocalInterfaceAddress(address) }.getOrDefault(false)
    }
}
