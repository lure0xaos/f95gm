package f95gm.server.app

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServerHealthStateTest {
    @Test
    fun livenessAndReadinessPropertiesTrackHealthTransitions() {
        val health = ServerHealthState()

        assertTrue(health.isLive)
        assertFalse(health.isReady)

        health.markServing()
        assertTrue(health.isLive)
        assertTrue(health.isReady)

        health.markStopping()
        assertTrue(health.isLive)
        assertFalse(health.isReady)

        health.markStopped()
        assertFalse(health.isLive)
        assertFalse(health.isReady)
    }

    @Test
    fun failedHealthStateIsNeitherLiveNorReady() {
        val health = ServerHealthState()

        health.markFailed()

        assertFalse(health.isLive)
        assertFalse(health.isReady)
    }
}
