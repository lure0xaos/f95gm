package f95gm.server.upstream.media

import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class PinnedAddressConnectorTest {
    @Test
    fun fallsBackAcrossValidatedAddresses() {
        val first = address(1)
        val second = address(2)
        val attempts = mutableListOf<InetAddress>()
        val connected = Socket()
        val result = PinnedAddressConnector(PinnedSocketConnector { address, _, _ ->
            attempts += address
            if (address == first) throw IOException("first address unavailable")
            connected
        }).connect(listOf(first, second), 443, 1_000)

        assertSame(connected, result)
        assertEquals(listOf(first, second), attempts)
        connected.close()
    }

    @Test
    fun reportsTheLastConnectionFailureWhenAllAddressesFail() {
        val first = address(1)
        val second = address(2)
        val error = assertFailsWith<IOException> {
            PinnedAddressConnector(PinnedSocketConnector { address, _, _ ->
                throw IOException("failed " + address.hostAddress)
            }).connect(listOf(first, second), 443, 1_000)
        }

        assertEquals("failed " + second.hostAddress, error.message)
    }

    @Test
    fun rejectsAnEmptyValidatedAddressSet() {
        val error = assertFailsWith<IOException> {
            PinnedAddressConnector(PinnedSocketConnector { _, _, _ -> error("not called") })
                .connect(emptyList(), 443, 1_000)
        }

        assertEquals("No validated media address was available", error.message)
    }

    private fun address(lastOctet: Int): InetAddress =
        InetAddress.getByAddress(byteArrayOf(192.toByte(), 0, 2, lastOctet.toByte()))
}
