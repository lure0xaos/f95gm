package f95gm.server.http.request

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class IdempotencyFingerprintServiceTest {
    @Test
    fun objectPropertyOrderDoesNotChangeFingerprint() {
        assertEquals(
            IdempotencyFingerprintService.digest("{\"b\":2,\"a\":1}"),
            IdempotencyFingerprintService.digest("{\"a\":1,\"b\":2}")
        )
    }

    @Test
    fun changedPayloadChangesFingerprint() {
        assertNotEquals(
            IdempotencyFingerprintService.digest("{\"a\":1}"),
            IdempotencyFingerprintService.digest("{\"a\":2}")
        )
    }

    @Test
    fun preservesArrayOrderButNormalizesWhitespaceAndInvalidPayloads() {
        assertNotEquals(IdempotencyFingerprintService.digest("[1,2]"), IdempotencyFingerprintService.digest("[2,1]"))
        assertEquals(IdempotencyFingerprintService.digest(" { \"a\": 1 } "), IdempotencyFingerprintService.digest("{\"a\":1}"))
        assertEquals(IdempotencyFingerprintService.digest("not-json"), IdempotencyFingerprintService.digest(" not-json "))
    }
}
