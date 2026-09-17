package f95gm.server.app

import f95gm.server.desktop.DesktopLifecycle
import f95gm.server.desktop.DesktopTheme
import f95gm.server.desktop.NoopDesktopLifecycle
import f95gm.server.state.runtime.StartupMetrics
import io.ktor.server.engine.ApplicationEngine
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertSame

class ServerRuntimeStateTest {
    @Test
    fun bindsServerThenDesktopAndClosesOwnedResourcesExactlyOnce() {
        val events = mutableListOf<String>()
        val runtime = ServerRuntimeState(StartupMetrics())
        val engine = Proxy.newProxyInstance(
            ApplicationEngine::class.java.classLoader,
            arrayOf(ApplicationEngine::class.java)
        ) { _, method, _ ->
            if (method.name == "stop") events += "server"
            null
        } as ApplicationEngine
        val desktop = object : DesktopLifecycle {
            override fun start() = Unit
            override fun updateTheme(theme: DesktopTheme?) = Unit
            override fun dispose() { events += "desktop" }
        }

        assertEquals(ServerRuntimePhase.NEW, runtime.phase)
        runtime.bind(engine)
        assertEquals(ServerRuntimePhase.SERVER_BOUND, runtime.phase)
        runtime.bind(desktop)
        assertEquals(ServerRuntimePhase.RUNNING, runtime.phase)
        assertTrue(runtime.desktopController() === desktop)
        assertFalse(runtime.isClosed)

        runtime.close()
        runtime.close()

        assertEquals(ServerRuntimePhase.CLOSED, runtime.phase)
        assertTrue(runtime.isClosed)
        assertEquals(listOf("desktop", "server"), events)
    }

    @Test
    fun rejectsResourcesBoundOutOfOrderAndKeepsDefaultDesktopBeforeBinding() {
        val runtime = ServerRuntimeState(StartupMetrics())
        assertSame(NoopDesktopLifecycle, runtime.desktopController())
        val desktop = object : DesktopLifecycle {
            override fun start() = Unit
            override fun updateTheme(theme: DesktopTheme?) = Unit
            override fun dispose() = Unit
        }
        assertFailsWith<IllegalStateException> { runtime.bind(desktop) }
    }
}
