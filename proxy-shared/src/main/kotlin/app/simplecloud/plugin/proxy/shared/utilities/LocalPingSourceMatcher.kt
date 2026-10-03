package app.simplecloud.plugin.proxy.shared.utilities

import java.net.InetAddress
import java.net.NetworkInterface

object LocalPingSourceMatcher {

    fun isLocal(address: InetAddress?): Boolean {
        if (address == null) {
            return false
        }

        if (address.isLoopbackAddress) {
            return true
        }

        return try {
            NetworkInterface.getByInetAddress(address) != null
        } catch (_: Exception) {
            false
        }
    }
}