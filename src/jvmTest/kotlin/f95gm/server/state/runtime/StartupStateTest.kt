package f95gm.server.state.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StartupStateTest {
    @Test
    fun acceptsTheSuccessfulStartupLifecycleAndRejectsInvalidTransitions() {
        val startup = StartupState()
        assertEquals(StartupStage.CREATED, startup.current())
        startup.advance(StartupStage.CONFIGURED)
        startup.advance(StartupStage.DATABASE_READY)
        startup.advance(StartupStage.COMPOSED)
        startup.advance(StartupStage.SERVING)
        startup.stop()
        assertEquals(StartupStage.STOPPED, startup.current())
        assertFailsWith<IllegalStateException> { startup.advance(StartupStage.SERVING) }
    }

    @Test
    fun failureCanFollowStartupAndMustBeStoppedAfterward() {
        val startup = StartupState()
        startup.fail()
        assertEquals(StartupStage.FAILED, startup.current())
        startup.stop()
        assertEquals(StartupStage.STOPPED, startup.current())
        assertFailsWith<IllegalStateException> { startup.fail() }
    }

    @Test
    fun classifiesFailuresByTheLastCompletedStartupStage() {
        assertEquals(StartupFailureCategory.CONFIGURATION, StartupStage.CREATED.failureCategory())
        assertEquals(StartupFailureCategory.DATABASE, StartupStage.CONFIGURED.failureCategory())
        assertEquals(StartupFailureCategory.COMPOSITION, StartupStage.DATABASE_READY.failureCategory())
        assertEquals(StartupFailureCategory.BINDING, StartupStage.COMPOSED.failureCategory())
        assertEquals(StartupFailureCategory.RUNTIME, StartupStage.SERVING.failureCategory())
        assertEquals(StartupFailureCategory.RUNTIME, StartupStage.STOPPED.failureCategory())
    }
}
