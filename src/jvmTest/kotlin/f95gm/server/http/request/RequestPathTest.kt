package f95gm.server.http.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RequestPathTest {
    @Test
    fun removesQueryStringsButPreservesThePath() {
        assertEquals("/api/catalog", RequestPath.from("/api/catalog?page=2").value)
        assertEquals("/", RequestPath.from("/").value)
    }

    @Test
    fun rejectsPathsThatAreNotAbsolute() {
        assertFailsWith<IllegalArgumentException> { RequestPath("api/catalog") }
    }
}
