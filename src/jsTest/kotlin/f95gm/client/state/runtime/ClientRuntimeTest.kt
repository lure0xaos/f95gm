package f95gm.client.state.runtime

import f95gm.client.actions.auth.CredentialCommand
import f95gm.client.actions.effects.ClientUiEffects
import f95gm.client.routing.navigation.TypedRoute
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.promise
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ClientRuntimeTest {
    @Test
    fun closeCancelsStoresAndOwnedHttpAndRejectsRestart() {
        val http = HttpClient(MockEngine { error("Unexpected HTTP request") })
        val runtime = runtime(http, HttpClientOwnership.Owned)
        runtime.start()
        runtime.start()
        runtime.close()
        runtime.close()
        assertTrue(runtime.state.controller.rootStoreJob.isCancelled)
        assertFalse(checkNotNull(http.coroutineContext[Job]).isActive)
        assertFailsWith<IllegalStateException> { runtime.start() }
    }

    @Test
    fun closeBeforeStartLeavesBorrowedHttpAvailableToItsOwner() {
        val http = HttpClient(MockEngine { error("Unexpected HTTP request") })
        try {
            val runtime = runtime(http, HttpClientOwnership.Borrowed)
            runtime.close()
            runtime.close()
            assertTrue(checkNotNull(http.coroutineContext[Job]).isActive)
            assertFailsWith<IllegalStateException> { runtime.start() }
        } finally {
            http.close()
        }
    }

    @Test
    fun loginUiCommandsUpdateCredentialsAndPreserveValidation() = CoroutineScope(Dispatchers.Default).promise {
        val http = HttpClient(MockEngine { error("Invalid credentials must not reach HTTP") })
        val runtime = runtime(http, HttpClientOwnership.Owned)
        val ui: ClientLoginUiPort = runtime
        runtime.start()
        try {
            ui.loginCommands.updateCredentials(CredentialCommand.SetLogin("tester"))
            withTimeout(2.seconds) { ui.credentialsView.data.first { it.login == "tester" } }
            ui.loginCommands.signIn()
            val failed = withTimeout(2.seconds) { ui.sessionView.data.first { it.async is AsyncState.Failure } }
            assertFalse(failed.authenticated)
            assertEquals("tester", ui.credentialsView.current.login)
        } finally {
            runtime.close()
        }
    }

    @Test
    fun featureLoadingPortsStayIndependentAndSessionResetClearsAllViews() {
        val http = HttpClient(MockEngine { error("Unexpected HTTP request") })
        val runtime = runtime(http, HttpClientOwnership.Owned)
        val catalog: ClientCatalogActionsPort = runtime
        val marks: ClientMarksActionsPort = runtime
        val filters: ClientFiltersActionsPort = runtime
        val detail: ClientDetailActionsPort = runtime
        val auth: ClientAuthActionsPort = runtime
        try {
            catalog.catalogLoading.markLoaded()
            assertTrue(catalog.catalogLoading.isLoaded())
            assertFalse(marks.marksLoading.isLoaded())
            filters.filtersLoading.markLoaded()
            filters.filtersLoading.clearLoaded()
            assertFalse(filters.filtersLoading.isLoaded())
            assertTrue(catalog.catalogLoading.isLoaded())
            marks.marksLoading.markLoaded()
            detail.detailLoading.loadedDetailId = f95gm.domain.values.F95ThreadId("42")
            catalog.catalogLoading.loadedOptionsCategory = f95gm.domain.filters.F95Category.GAMES
            filters.filtersLoading.appliedFilterId = f95gm.domain.values.F95FilterId("saved")
            auth.sessionLifecycle.resetSessionLifecycle()
            assertFalse(catalog.catalogLoading.isLoaded())
            assertFalse(marks.marksLoading.isLoaded())
            assertEquals(null, detail.detailLoading.loadedDetailId)
            assertEquals(null, catalog.catalogLoading.loadedOptionsCategory)
            assertEquals(null, filters.filtersLoading.appliedFilterId)
        } finally {
            runtime.close()
        }
    }

    @Test
    fun requestPortRejectsStaleResultsAndCancellationInvalidatesCurrentRequest() {
        val http = HttpClient(MockEngine { error("Unexpected HTTP request") })
        val runtime = runtime(http, HttpClientOwnership.Owned)
        val actions: ClientCatalogActionsPort = runtime
        try {
            val first = actions.requests.beginRequest(ClientJobKey.CATALOG_LOAD)
            val second = actions.requests.beginRequest(ClientJobKey.CATALOG_LOAD)
            assertFalse(actions.requests.isCurrentRequest(ClientJobKey.CATALOG_LOAD, first))
            assertTrue(actions.requests.isCurrentRequest(ClientJobKey.CATALOG_LOAD, second))
            actions.requests.cancelJob(ClientJobKey.CATALOG_LOAD)
            assertFalse(actions.requests.isCurrentRequest(ClientJobKey.CATALOG_LOAD, second))
            val connection: ClientConnectionActionsPort = runtime
            assertEquals(0, connection.healthProbes.nextProbeNumber())
            assertEquals(1, connection.healthProbes.nextProbeNumber())
        } finally {
            runtime.close()
        }
    }

    @Test
    fun repeatedFilterOptionsLoadKeepsTheActiveRequestAlive() = CoroutineScope(Dispatchers.Default).promise {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var calls = 0
        val http = HttpClient(MockEngine {
            calls++
            started.complete(Unit)
            release.await()
            respond("""{"tags":{"11":"Adventure"},"prefixes":[]}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"))
        })
        val runtime = runtime(http, HttpClientOwnership.Owned)
        runtime.start()
        try {
            runtime.catalogCommands.loadFilterOptionsIfNeeded()
            withTimeout(2.seconds) { started.await() }
            runtime.catalogCommands.loadFilterOptionsIfNeeded()
            release.complete(Unit)
            val loaded = withTimeout(2.seconds) { runtime.state.features.catalogOptions.data.first { it.tags.size == 1 } }
            assertEquals("Adventure", loaded.tags.single().name.value)
            assertEquals(1, calls)
        } finally {
            release.complete(Unit)
            runtime.close()
        }
    }

    private fun runtime(http: HttpClient, ownership: HttpClientOwnership): ClientRuntime {
        val values = mutableMapOf<String, String>()
        val port = object : BrowserStoragePort {
            override fun read(key: String): String? = values[key]
            override fun write(key: String, value: String) { values[key] = value }
        }
        val browser = object : BrowserEffects by WindowBrowserEffects {
            override val storage = BrowserStorage(object : BrowserStorageRuntime {
                override val localStorage = port
                override val sessionStorage = port
            })
        }
        val effects = object : ClientUiEffects {
            override fun navigate(route: TypedRoute) = Unit
            override suspend fun toast(message: String) = Unit
        }
        return ClientRuntime(
            ClientState(browser = browser),
            ClientRuntimeOverrides(uiEffects = effects),
            ClientRuntimeDependencies(http, ownership, SystemClientClock)
        )
    }
}
