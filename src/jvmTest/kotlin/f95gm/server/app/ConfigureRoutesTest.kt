package f95gm.server.app

import f95gm.domain.api.*
import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95SessionId
import f95gm.server.config.Bytes
import f95gm.server.config.ServerConfig
import f95gm.server.desktop.ServerShutdownController
import f95gm.server.http.request.ShutdownToken
import f95gm.server.http.request.ratelimit.RateLimitPolicy
import f95gm.server.model.auth.UpstreamSession
import f95gm.server.persistence.database.DatabaseExecutor
import f95gm.server.state.runtime.ServerContext
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import java.io.IOException
import kotlin.test.*

private val operationalEndpoints = setOf(F95ApiPaths.HEALTH, F95ApiPaths.LIVE, F95ApiPaths.READY, F95ApiPaths.METRICS, F95ApiPaths.SHUTDOWN)

class ConfigureRoutesTest {
    @Test
    fun routeConfigurationKeepsTypedRequestAndBucketLimits() {
        val configuration = RouteConfiguration(
            maxRequestBodyBytes = Bytes(4096),
            maxRateLimitBuckets = RateLimitBucketLimit(12)
        )

        assertEquals(4096, configuration.maxRequestBodyBytes.value)
        assertEquals(12, configuration.maxRateLimitBuckets.value)
    }

    @Test
    fun configuredRoutesMatchTheSharedEndpointContract() {
        val configured = configuredApiEndpoints.map { it.path to it.method.wireValue }.toSet()
        val declared = endpointDescriptors.filterNot { it.path.value in operationalEndpoints }.flatMap { descriptor ->
            descriptor.methods.map { method -> descriptor.path.value to method.wireValue }
        }.toSet()
        assertEquals(
            declared,
            configured,
            "declared-only=${declared - configured}; configured-only=${configured - declared}"
        )
        assertEquals(
            endpointDescriptors.filterNot { it.path.value in operationalEndpoints }
                .flatMap { descriptor -> descriptor.methods.map { descriptor.path.value to it.wireValue } }
                .groupingBy { it }.eachCount(),
            configuredApiEndpoints.map { it.path to it.method.wireValue }.groupingBy { it }.eachCount(),
            "Each declared method must have exactly one route handler"
        )
        assertEquals(
            configuredApiEndpoints.size,
            configuredApiEndpoints.map { it.path to it.method.wireValue }.distinct().size,
            "Each route key must have exactly one handler"
        )
        configuredApiEndpoints.forEach { endpoint ->
            assertTrue(endpoint.contractDescriptor.methods.any { it.wireValue == endpoint.method.wireValue })
        }
        val generatedContract = openApiContractJson()
        configuredApiEndpoints.forEach { endpoint ->
            assertContains(generatedContract, "\"${endpoint.path}\"")
            assertContains(generatedContract, "\"${endpoint.method.wireValue.lowercase()}\"")
        }
    }

    @Test
    fun endpointHandlerInventoryHasNoDuplicateContractKeys() {
        val keys = configuredApiEndpoints.map { it.path to it.method.wireValue }
        assertEquals(keys.size, keys.toSet().size, "A contract key must have one handler")
        assertEquals(
            endpointDescriptors.filterNot { it.path.value in operationalEndpoints }.flatMap { descriptor ->
                descriptor.methods.map { descriptor.path.value to it.wireValue }
            }.toSet(),
            keys.toSet()
        )
    }

    @Test
    fun openApiOperationsParametersAndSchemaReferencesMatchRegisteredRoutes() {
        val contract = Json.parseToJsonElement(openApiContractJson()).jsonObject
        val paths = contract.requiredObject("paths")
        val registeredRoutes = configuredApiEndpoints.filterNot { it.path in operationalEndpoints }
            .map { it.path to it.method.wireValue.lowercase() }.toSet()
        val documentedRoutes = paths.filterKeys { it !in operationalEndpoints }.flatMap { (path, operations) ->
            operations.jsonObject.keys.map { method -> path to method }
        }.toSet()
        assertEquals(registeredRoutes, documentedRoutes)

        endpointDescriptors.filterNot { it.path.value in operationalEndpoints }.forEach { descriptor ->
            descriptor.methods.forEach { method ->
                    val operation = paths.requiredObject(descriptor.path.value).requiredObject(method.wireValue.lowercase())
                val parameters = operation["parameters"]?.jsonArray.orEmpty().map { parameter ->
                    val value = parameter.jsonObject
                    value.requiredPrimitive("in").content to value.requiredPrimitive("name").content
                }.toSet()
                assertEquals(
                    descriptor.pathParameters.map { "path" to it.value }
                        .toSet() + descriptor.queryParameters.map { "query" to it.value }.toSet(),
                    parameters,
                    "${method.wireValue} ${descriptor.path.value} parameter metadata"
                )
            }
        }

        val schemas = contract.requiredObject("components").requiredObject("schemas")
        val unresolved = schemaReferences(contract).filter { reference ->
            reference.startsWith("#/components/schemas/") && reference.substringAfterLast('/') !in schemas
        }
        assertEquals(emptyList(), unresolved.distinct())
    }

    private fun schemaReferences(element: JsonElement): List<String> = when {
        element is kotlinx.serialization.json.JsonObject -> element.flatMap { (key, value) ->
            if (key == "\$ref") listOf(value.jsonPrimitive.content)
            else schemaReferences(value)
        }

        element is kotlinx.serialization.json.JsonArray -> element.flatMap(::schemaReferences)
        else -> emptyList()
    }

    @Test
    fun livenessRemainsAvailableWhileReadinessRejectsDrainingServer() = testApplication {
        val shutdownController = ServerShutdownController {}
        application {
            configureTestRoutes("shutdown-token", { shutdownController }, { f95gm.server.desktop.NoopDesktopLifecycle })
        }

        assertEquals(HttpStatusCode.OK, client.get(F95ApiPaths.LIVE).status)
        shutdownController.request()
        assertEquals(HttpStatusCode.ServiceUnavailable, client.get(F95ApiPaths.READY).status)
    }

    @Test
    fun itemRouteUsesTheInjectedFakeUpstream() = testApplication {
        val upstream = HttpClient(MockEngine {
            respond(
                content = """
                    <html><head><meta property="og:title" content="Fake game"></head>
                    <body><article class="message-body"><div class="bbWrapper">Description</div></article></body></html>
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Html.toString())
            )
        })
        val context = ServerContext(
            ServerConfig.from(emptyMap()),
            httpClient = upstream,
            databaseExecutor = DatabaseExecutor(database = { error("database not used") }, executionContext = f95gm.server.persistence.database.testDatabaseExecutionContext())
        )
        val sessionId = F95SessionId("00000000-0000-0000-0000-000000000001")
        context.applicationComposition.infrastructure.sessions.put(sessionId, UpstreamSession(sessionId, F95AccountKey("user")))
        val shutdownController = ServerShutdownController {}
        application {
            configureRoutes(
                ShutdownToken("shutdown-token"),
                { shutdownController },
                RouteConfiguration(),
                context
            )
        }

        try {
            val invalidId = client.get("${F95ApiPaths.ITEM}/not-a-number") {
                header(HttpHeaders.Cookie, "${f95gm.server.config.protocol.SessionProtocolConfig.BROWSER_COOKIE}=${sessionId.value}")
            }
            assertEquals(HttpStatusCode.BadRequest, invalidId.status)

            val response = client.get("${F95ApiPaths.ITEM}/123") {
                header(HttpHeaders.Cookie, "${f95gm.server.config.protocol.SessionProtocolConfig.BROWSER_COOKIE}=${sessionId.value}")
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Fake game"))
        } finally {
            shutdownController.close()
            context.close()
        }
    }

    @Test
    fun itemRouteMapsMissingUpstreamItemsToNotFound() = testApplication {
        val upstream = HttpClient(MockEngine {
            respond("<html><body>missing</body></html>", HttpStatusCode.NotFound, headersOf(HttpHeaders.ContentType, ContentType.Text.Html.toString()))
        })
        val context = ServerContext(
            ServerConfig.from(emptyMap()),
            httpClient = upstream,
            databaseExecutor = DatabaseExecutor(database = { error("database not used") }, executionContext = f95gm.server.persistence.database.testDatabaseExecutionContext())
        )
        val sessionId = F95SessionId("00000000-0000-0000-0000-000000000021")
        context.applicationComposition.infrastructure.sessions.put(sessionId, UpstreamSession(sessionId, F95AccountKey("user")))
        application {
            configureStatusPages()
            configureRoutes(ShutdownToken("shutdown-token"), { ServerShutdownController {} }, RouteConfiguration(), context)
        }
        try {
            val response = client.get("${F95ApiPaths.ITEM}/123") {
                header(HttpHeaders.Cookie, "${f95gm.server.config.protocol.SessionProtocolConfig.BROWSER_COOKIE}=${sessionId.value}")
            }
            assertEquals(HttpStatusCode.NotFound, response.status)
            assertEquals(F95ApiMessageKey("server.itemLoadFailed"), Json.decodeFromString<F95ApiError>(response.bodyAsText()).messageKey)
        } finally {
            context.close()
        }
    }

    @Test
    fun upstreamTransportFailuresUseTheLocalizedApiEnvelope() = testApplication {
        val upstream = HttpClient(MockEngine { throw IOException("offline") })
        val context = ServerContext(
            ServerConfig.from(emptyMap()),
            httpClient = upstream,
            databaseExecutor = DatabaseExecutor(database = { error("database not used") }, executionContext = f95gm.server.persistence.database.testDatabaseExecutionContext())
        )
        val sessionId = F95SessionId("00000000-0000-0000-0000-000000000003")
        context.applicationComposition.infrastructure.sessions.put(sessionId, UpstreamSession(sessionId, F95AccountKey("user")))
        application {
            configureStatusPages()
            configureRoutes(
                ShutdownToken("shutdown-token"),
                { ServerShutdownController {} },
                RouteConfiguration(),
                context
            )
        }

        try {
            val response = client.get("${F95ApiPaths.ITEM}/123") {
                header(HttpHeaders.Cookie, "${f95gm.server.config.protocol.SessionProtocolConfig.BROWSER_COOKIE}=${sessionId.value}")
            }
            val error = Json.decodeFromString<F95ApiError>(response.bodyAsText())
            assertEquals(HttpStatusCode.BadGateway, response.status)
            assertEquals(F95ApiErrorCode.UPSTREAM_FAILURE, error.code)
            assertEquals(F95ApiMessageKey("server.upstreamFailure"), error.messageKey)
            assertEquals("CONNECTION_FAILURE", error.details["reason"])
        } finally {
            context.close()
        }
    }

    @Test
    fun authenticatedRequestsExposeIndependentAccountRateLimitHeaders() = testApplication {
        val upstream = HttpClient(MockEngine { respond("[]", HttpStatusCode.OK) })
        val context = ServerContext(
            ServerConfig.from(emptyMap()),
            httpClient = upstream,
            databaseExecutor = DatabaseExecutor(database = { error("database not used") }, executionContext = f95gm.server.persistence.database.testDatabaseExecutionContext())
        )
        val sessionId = F95SessionId("00000000-0000-0000-0000-000000000004")
        context.applicationComposition.infrastructure.sessions.put(sessionId, UpstreamSession(sessionId, F95AccountKey("user")))
        application {
            configureStatusPages()
            configureRoutes(
                ShutdownToken("shutdown-token"),
                { ServerShutdownController {} },
                RouteConfiguration(rateLimitPolicy = RateLimitPolicy.configured(4, 2, 60_000L)),
                context
            )
        }

        try {
            val response = client.get(F95ApiPaths.CATALOG) {
                header(HttpHeaders.Cookie, "${f95gm.server.config.protocol.SessionProtocolConfig.BROWSER_COOKIE}=${sessionId.value}")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("4", response.headers[f95gm.server.config.protocol.RateLimitProtocolConfig.IP_LIMIT])
            assertEquals("3", response.headers[f95gm.server.config.protocol.RateLimitProtocolConfig.IP_REMAINING])
            assertEquals("2", response.headers[f95gm.server.config.protocol.RateLimitProtocolConfig.ACCOUNT_LIMIT])
            assertEquals("1", response.headers[f95gm.server.config.protocol.RateLimitProtocolConfig.ACCOUNT_REMAINING])
            assertNotNull(response.headers[f95gm.server.config.protocol.RateLimitProtocolConfig.ACCOUNT_RESET])
        } finally {
            context.close()
        }
    }

    @Test
    fun catalogOptionsSupportsConditionalResponses() = testApplication {
        val upstream = HttpClient(MockEngine {
            respond(
                "<script>var latestUpdates = {\"tags\":{},\"prefixes\":{}};</script>",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, ContentType.Text.Html.toString())
            )
        })
        val context = ServerContext(
            ServerConfig.from(emptyMap()),
            httpClient = upstream,
            databaseExecutor = DatabaseExecutor(database = { error("database not used") }, executionContext = f95gm.server.persistence.database.testDatabaseExecutionContext())
        )
        val sessionId = F95SessionId("00000000-0000-0000-0000-000000000005")
        context.applicationComposition.infrastructure.sessions.put(sessionId, UpstreamSession(sessionId, F95AccountKey("user")))
        application {
            configureStatusPages()
            configureRoutes(
                ShutdownToken("shutdown-token"),
                { ServerShutdownController {} },
                RouteConfiguration(),
                context
            )
        }

        try {
            val cookie = "${f95gm.server.config.protocol.SessionProtocolConfig.BROWSER_COOKIE}=${sessionId.value}"
            val first = client.get(F95ApiPaths.CATALOG_OPTIONS) { header(HttpHeaders.Cookie, cookie) }
            val etag = first.headers[HttpHeaders.ETag]
            assertEquals(HttpStatusCode.OK, first.status)
            assertNotNull(etag)
            val second = client.get(F95ApiPaths.CATALOG_OPTIONS) {
                header(HttpHeaders.Cookie, cookie)
                header(HttpHeaders.IfNoneMatch, "W/$etag")
            }
            assertEquals(HttpStatusCode.NotModified, second.status)
            assertEquals(etag, second.headers[HttpHeaders.ETag])
        } finally {
            context.close()
        }
    }
}
