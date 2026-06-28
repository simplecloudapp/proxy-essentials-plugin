package app.simplecloud.plugin.proxy.shared.handler

import java.net.InetAddress
import java.net.NetworkInterface

class TrustedPingSourceMatcher(
    private val trustedSources: () -> List<String>,
    private val warn: (String) -> Unit
) {
    private val localAddresses: Set<InetAddress> by lazy { discoverLocalAddresses() }

    @Volatile
    private var cachedSourceEntries: List<String> = emptyList()

    @Volatile
    private var cachedRanges: List<TrustedAddressRange> = emptyList()

    fun isTrusted(remoteAddress: InetAddress?): Boolean {
        if (remoteAddress == null) return false
        if (remoteAddress.isLoopbackAddress || localAddresses.contains(remoteAddress)) return true

        return ranges().any { it.contains(remoteAddress) }
    }

    private fun ranges(): List<TrustedAddressRange> {
        val currentEntries = trustedSources().map { it.trim() }.filter { it.isNotEmpty() }
        if (currentEntries == cachedSourceEntries) {
            return cachedRanges
        }

        val parsedRanges = currentEntries.mapNotNull { entry ->
            TrustedAddressRange.parse(entry) ?: run {
                warn("Ignoring invalid trusted ping source '$entry'. Expected an IP address or CIDR range.")
                null
            }
        }

        cachedSourceEntries = currentEntries
        cachedRanges = parsedRanges
        return parsedRanges
    }

    private fun discoverLocalAddresses(): Set<InetAddress> {
        val addresses = mutableSetOf<InetAddress>()

        runCatching {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .filter { it.isUp }
                .flatMap { it.inetAddresses.asSequence() }
                .forEach { addresses.add(it) }
        }.onFailure {
            warn("Unable to discover local network addresses: ${it.message}")
        }

        runCatching { addresses.add(InetAddress.getLocalHost()) }
        addresses.add(InetAddress.getLoopbackAddress())

        return addresses
    }

    private data class TrustedAddressRange(
        private val addressBytes: ByteArray,
        private val prefixLength: Int
    ) {
        fun contains(address: InetAddress): Boolean {
            val candidateBytes = address.address
            if (candidateBytes.size != addressBytes.size) return false

            val fullBytes = prefixLength / BITS_PER_BYTE
            val remainingBits = prefixLength % BITS_PER_BYTE

            for (index in 0 until fullBytes) {
                if (candidateBytes[index] != addressBytes[index]) return false
            }

            if (remainingBits == 0) return true

            val mask = (0xFF shl (BITS_PER_BYTE - remainingBits)) and 0xFF
            return (candidateBytes[fullBytes].toInt() and mask) == (addressBytes[fullBytes].toInt() and mask)
        }

        companion object {
            private const val BITS_PER_BYTE = 8

            fun parse(entry: String): TrustedAddressRange? {
                val parts = entry.split('/', limit = 2).map { it.trim() }
                val address = runCatching { InetAddress.getByName(parts[0]) }.getOrNull() ?: return null
                val maxPrefixLength = address.address.size * BITS_PER_BYTE
                val prefixLength = when (parts.size) {
                    1 -> maxPrefixLength
                    else -> parts[1].toIntOrNull() ?: return null
                }

                if (prefixLength !in 0..maxPrefixLength) return null

                return TrustedAddressRange(address.address, prefixLength)
            }
        }
    }
}
