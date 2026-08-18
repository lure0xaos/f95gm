package f95gm.server.desktop.console

import java.io.OutputStream

internal class TeeOutputStream(
    private val first: OutputStream,
    private val second: OutputStream
) : OutputStream() {
    override fun write(byte: Int) {
        first.write(byte)
        second.write(byte)
    }

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        first.write(bytes, offset, length)
        second.write(bytes, offset, length)
    }

    override fun flush() {
        first.flush()
        second.flush()
    }
}
