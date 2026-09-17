package f95gm.server.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ConfigurationValidationTest {
    @Test
    fun normalizesValidHttpsOrigin() {
        assertEquals("https://f95zone.to", ConfigurationValidation.upstreamOrigin("https://f95zone.to/"))
    }

    @Test
    fun permitsLoopbackHttpOnlyForFixtures() {
        assertEquals("http://127.0.0.1:8081", ConfigurationValidation.upstreamOrigin("http://127.0.0.1:8081"))
    }

    @Test
    fun rejectsCredentialsAndQueryParameters() {
        assertFailsWith<IllegalArgumentException> {
            ConfigurationValidation.upstreamOrigin("https://user:secret@f95zone.to/?x=1")
        }
    }
}
