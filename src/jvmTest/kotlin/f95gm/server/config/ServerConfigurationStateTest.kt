package f95gm.server.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlinx.coroutines.runBlocking
import kotlin.test.assertTrue

class ServerConfigurationStateTest {
    @Test
    fun reloadPlannerSeparatesRevisionAndPolicyFromStateMutation() {
        val previous = ServerConfigSnapshot.from(ServerConfig.from(emptyMap()))
        val unchanged = ConfigurationReloadPlanner.plan(previous, ServerConfigSnapshot.from(ServerConfig.from(emptyMap())))
        assertTrue(unchanged.unchanged)
        assertEquals(previous.revision + 1, unchanged.next.revision)
        assertEquals(ConfigurationReloadDecision.Apply, unchanged.decision)
        assertTrue(unchanged.changes.isEmpty)

        val dynamic = ConfigurationReloadPlanner.plan(
            previous,
            ServerConfigSnapshot.from(ServerConfig.from(mapOf("f95gm.openBrowser" to "false")))
        )
        assertTrue(dynamic.changed)
        assertTrue(ConfigurationChangeField.OPEN_BROWSER in dynamic.changes.fields)
        assertEquals(ConfigurationReloadDecision.Apply, dynamic.decision)

        val restart = ConfigurationReloadPlanner.plan(
            previous,
            ServerConfigSnapshot.from(ServerConfig.from(mapOf("f95gm.port" to "18081")))
        )
        assertTrue(restart.decision is ConfigurationReloadDecision.RequiresRestart)
    }

    @Test
    fun reloadPublishesOneCompleteSnapshot() {
        val state = ServerConfigurationState(ServerConfigSnapshot.from(ServerConfig.from(emptyMap())))
        val reloaded = state.reload(object : ConfigurationSource {
            override fun properties() = mapOf("f95gm.openBrowser" to "false")
            override fun environment(): Map<String, String> = emptyMap()
        })

        assertEquals(false, reloaded.value.openBrowser)
        assertEquals(reloaded, state.current())
        assertEquals(ConfigurationReloadOutcome.Applied, state.reloadWithResult(object : ConfigurationSource {
            override fun properties() = emptyMap<String, String>()
            override fun environment(): Map<String, String> = emptyMap()
        }).outcome)
    }

    @Test
    fun unchangedReloadHasAnExplicitOutcome() {
        val state = ServerConfigurationState(ServerConfigSnapshot.from(ServerConfig.from(emptyMap())))

        val result = state.reloadWithResult(object : ConfigurationSource {
            override fun properties() = emptyMap<String, String>()
            override fun environment(): Map<String, String> = emptyMap()
        })

        assertEquals(ConfigurationReloadOutcome.Unchanged, result.outcome)
        assertEquals(false, result.changed)
    }

    @Test
    fun reloadRejectsChangesThatRequireRebuildingTheServer() {
        val state = ServerConfigurationState(ServerConfigSnapshot.from(ServerConfig.from(emptyMap())))

        assertFailsWith<IllegalArgumentException> {
            state.reload(object : ConfigurationSource {
                override fun properties() = mapOf("f95gm.port" to "18081")
                override fun environment(): Map<String, String> = emptyMap()
            })
        }
        assertEquals(8_080, state.current().value.port.value)
    }

    @Test
    fun listenerFailurePolicyIsInjectedAndDoesNotBlockPublication() {
        var observed: Exception? = null
        val listener = ConfigurationReloadListener { error("listener failure") }
        val state = ServerConfigurationState(
            ServerConfigSnapshot.from(ServerConfig.from(emptyMap())),
            ConfigurationListenerFailureHandler { failedListener, failure ->
                assertSame(listener, failedListener)
                observed = failure
            }
        )
        state.subscribe(listener)

        val result = state.reloadWithResult(object : ConfigurationSource {
            override fun properties() = mapOf("f95gm.openBrowser" to "false")
            override fun environment(): Map<String, String> = emptyMap()
        })

        assertEquals("listener failure", observed?.message)
        assertEquals(1, state.listenerFailureCount)
        assertEquals(1, result.listenerFailures)
        assertTrue(ConfigurationChangeField.OPEN_BROWSER in result.changes.fields)
        assertSame(result.changes, result.copy(listenerFailures = 0).changes)
        assertEquals(ConfigurationPublicationStatus.LISTENER_FAILURES, result.publicationStatus)
    }

    @Test
    fun repeatedAwaitableCloseLeavesConfigurationClosed(): Unit = runBlocking {
        val state = ServerConfigurationState(ServerConfigSnapshot.from(ServerConfig.from(emptyMap())))

        state.closeAndJoin()
        state.closeAndJoin()

        assertTrue(state.isClosed)
        assertFailsWith<IllegalStateException> {
            state.reload(object : ConfigurationSource {
                override fun properties() = emptyMap<String, String>()
                override fun environment(): Map<String, String> = emptyMap()
            })
        }
    }
}
