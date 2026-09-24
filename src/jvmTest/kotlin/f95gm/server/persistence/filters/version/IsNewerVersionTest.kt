package f95gm.server.persistence.filters.version

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsNewerVersionTest {
    @Test
    fun comparesNumericVersions() {
        assertTrue(isNewerVersion("1.10", "1.9"))
        assertFalse(isNewerVersion("1.9", "1.10"))
        assertFalse(isNewerVersion("1.0", "1.0"))
    }
}
