package f95gm.server.http.response

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class EntityTagPolicyTest {
    @Test
    fun createsStableBodyValidators() {
        assertEquals(EntityTagPolicy.forBody("same"), EntityTagPolicy.forBody("same"))
        assertNotEquals(EntityTagPolicy.forBody("same"), EntityTagPolicy.forBody("changed"))
        assertEquals('"', EntityTagPolicy.forBody("same").first())
        assertEquals('"', EntityTagPolicy.forBody("same").last())
    }
}
