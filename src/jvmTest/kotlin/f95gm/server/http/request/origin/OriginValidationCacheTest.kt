package f95gm.server.http.request.origin

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class OriginValidationCacheTest {
    @Test
    fun expiresEntriesAndRecomputesThem() {
        val cache = OriginValidationCache(ttl = 10.milliseconds)
        var evaluations = 0
        assertEquals(true, cache.getOrPut("origin", nowMillis = 0) { evaluations++; true })
        assertEquals(true, cache.getOrPut("origin", nowMillis = 5) { evaluations++; false })
        assertEquals(false, cache.getOrPut("origin", nowMillis = 10) { evaluations++; false })
        assertEquals(2, evaluations)
    }
}
