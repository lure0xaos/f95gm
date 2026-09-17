package f95gm.server.desktop.console

import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals

class TeeOutputStreamTest {
    @Test
    fun writesBothSingleAndSlicedBytesAndFlushesBothOutputs() {
        val first = ByteArrayOutputStream()
        val second = ByteArrayOutputStream()
        val tee = TeeOutputStream(first, second)
        tee.write('A'.code)
        val bytes = "skip-data-tail".encodeToByteArray()
        tee.write(bytes, 5, 4)
        tee.flush()
        assertContentEquals("Adata".encodeToByteArray(), first.toByteArray())
        assertContentEquals(first.toByteArray(), second.toByteArray())
    }
}
