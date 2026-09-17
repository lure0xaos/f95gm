package f95gm.server.http.request.origin

import kotlin.test.Test
import kotlin.test.assertEquals
import f95gm.server.model.EpochMillis
import kotlin.time.Duration.Companion.milliseconds

class OriginValidationCacheTest {
    @Test
    fun expiresEntriesAndRecomputesThem() {
        val cache = OriginValidationCache(ttl = 10.milliseconds)
        var evaluations = 0
        assertEquals(true, cache.getOrPut("origin", now = EpochMillis(0)) { evaluations++; true })
        assertEquals(true, cache.getOrPut("origin", now = EpochMillis(5)) { evaluations++; false })
        assertEquals(false, cache.getOrPut("origin", now = EpochMillis(10)) { evaluations++; false })
        assertEquals(2, evaluations)
    }
}
