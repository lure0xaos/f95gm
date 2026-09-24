package f95gm.server.app

import f95gm.domain.api.*
import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95SessionId
import f95gm.server.config.Bytes
import f95gm.server.config.ServerConfig
import f95gm.server.config.constants.ServerConstants
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

private val operationalEndpoints = setOf(F95Api.HEALTH, F95Api.LIVE, F95Api.READY, F95Api.METRICS, F95Api.SHUTDOWN)

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
        val configured = configuredApiEndpoints.map { it.path to it.method.value }.toSet()
        val declared = F95Api.endpointDescriptors.filterNot { it.path in operationalEndpoints }.flatMap { descriptor ->
            descriptor.methods.map { method -> descriptor.path to method.wireValue }
        }.toSet()
        assertEquals(
            declared,
            configured,
            "declared-only=${declared - configured}; configured-only=${configured - declared}"
        )
        assertEquals(
            F95Api.endpointDescriptors.filterNot { it.path in operationalEndpoints }
                .flatMap { descriptor -> descriptor.methods.map { descriptor.path to it.wireValue } }
                .groupingBy { it }.eachCount(),
            configuredApiEndpoints.map { it.path to it.method.value }.groupingBy { it }.eachCount(),
            "Each declared method must have exactly one route handler"
        )
        val generatedContract = F95Api.openApiContractJson()
        configuredApiEndpoints.forEach { endpoint ->
            assertContains(generatedContract, "\"${endpoint.path}\"")
            assertContains(generatedContract, "\"${endpoint.method.value.lowercase()}\"")
        }
    }

    @Test
    fun openApiOperationsParametersAndSchemaReferencesMatchRegisteredRoutes() {
        val contract = Json.parseToJsonElement(F95Api.openApiContractJson()).jsonObject
        val paths = contract["paths"]!!.jsonObject
        val registeredRoutes = configuredApiEndpoints.filterNot { it.path in operationalEndpoints }
            .map { it.path to it.method.value.lowercase() }.toSet()
        val documentedRoutes = paths.filterKeys { it !in operationalEndpoints }.flatMap { (path, operations) ->
            operations.jsonObject.keys.map { method -> path to method }
        }.toSet()
        assertEquals(registeredRoutes, documentedRoutes)

        F95Api.endpointDescriptors.filterNot { it.path in operationalEndpoints }.forEach { descriptor ->
            descriptor.methods.forEach { method ->
                val operation = paths[descriptor.path]!!.jsonObject[method.wireValue.lowercase()]!!.jsonObject
                val parameters = operation["parameters"]?.jsonArray.orEmpty().map { parameter ->
                    val value = parameter.jsonObject
                    value["in"]!!.jsonPrimitive.content to value["name"]!!.jsonPrimitive.content
                }.toSet()
                assertEquals(
                    descriptor.pathParameters.map { "path" to it }
                        .toSet() + descriptor.queryParameters.map { "query" to it }.toSet(),
                    parameters,
                    "${method.wireValue} ${descriptor.path} parameter metadata"
                )
            }
        }

        val schemas = contract["components"]!!.jsonObject["schemas"]!!.jsonObject
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
            configureTestRoutes("shutdown-token", { shutdownController }, { null })
        }

        assertEquals(HttpStatusCode.OK, client.get(F95Api.LIVE).status)
        shutdownController.request()
        assertEquals(HttpStatusCode.ServiceUnavailable, client.get(F95Api.READY).status)
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
            databaseExecutor = DatabaseExecutor(database = { error("database not used") })
        )
        val sessionId = F95SessionId("00000000-0000-0000-0000-000000000001")
        context.sessions.put(sessionId, UpstreamSession(sessionId, F95AccountKey("user")))
        val shutdownController = ServerShutdownController {}
        application {
            configureRoutes(
                ShutdownToken("shutdown-token"),
                { shutdownController },
                { null },
                RouteConfiguration(),
                context
            )
        }

        try {
            val response = client.get("${F95Api.ITEM}/123") {
                header(HttpHeaders.Cookie, "${ServerConstants.SESSION_COOKIE}=${sessionId.value}")
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Fake game"))
        } finally {
            shutdownController.close()
            context.close()
        }
    }

    @Test
    fun upstreamTransportFailuresUseTheLocalizedApiEnvelope() = testApplication {
        val upstream = HttpClient(MockEngine { throw IOException("offline") })
        val context = ServerContext(
            ServerConfig.from(emptyMap()),
            httpClient = upstream,
            databaseExecutor = DatabaseExecutor(database = { error("database not used") })
        )
        val sessionId = F95SessionId("00000000-0000-0000-0000-000000000003")
        context.sessions.put(sessionId, UpstreamSession(sessionId, F95AccountKey("user")))
        application {
            configureStatusPages()
            configureRoutes(
                ShutdownToken("shutdown-token"),
                { ServerShutdownController {} },
                { null },
                RouteConfiguration(),
                context
            )
        }

        try {
            val response = client.get("${F95Api.ITEM}/123") {
                header(HttpHeaders.Cookie, "${ServerConstants.SESSION_COOKIE}=${sessionId.value}")
            }
            val error = Json.decodeFromString<F95ApiError>(response.bodyAsText())
            assertEquals(HttpStatusCode.BadGateway, response.status)
            assertEquals(F95ApiErrorCode.UPSTREAM_FAILURE, error.code)
            assertEquals(F95ApiMessageKey("server.upstreamFailure"), error.messageKey)
            assertEquals("TRANSPORT", error.details["reason"])
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
            databaseExecutor = DatabaseExecutor(database = { error("database not used") })
        )
        val sessionId = F95SessionId("00000000-0000-0000-0000-000000000004")
        context.sessions.put(sessionId, UpstreamSession(sessionId, F95AccountKey("user")))
        application {
            configureStatusPages()
            configureRoutes(
                ShutdownToken("shutdown-token"),
                { ServerShutdownController {} },
                { null },
                RouteConfiguration(rateLimitPolicy = RateLimitPolicy.configured(4, 2, 60_000L)),
                context
            )
        }

        try {
            val response = client.get(F95Api.CATALOG) {
                header(HttpHeaders.Cookie, "${ServerConstants.SESSION_COOKIE}=${sessionId.value}")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("4", response.headers[ServerConstants.RATE_LIMIT_IP_LIMIT_HEADER])
            assertEquals("3", response.headers[ServerConstants.RATE_LIMIT_IP_REMAINING_HEADER])
            assertEquals("2", response.headers[ServerConstants.RATE_LIMIT_ACCOUNT_LIMIT_HEADER])
            assertEquals("1", response.headers[ServerConstants.RATE_LIMIT_ACCOUNT_REMAINING_HEADER])
            assertNotNull(response.headers[ServerConstants.RATE_LIMIT_ACCOUNT_RESET_HEADER])
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
            databaseExecutor = DatabaseExecutor(database = { error("database not used") })
        )
        val sessionId = F95SessionId("00000000-0000-0000-0000-000000000005")
        context.sessions.put(sessionId, UpstreamSession(sessionId, F95AccountKey("user")))
        application {
            configureStatusPages()
            configureRoutes(
                ShutdownToken("shutdown-token"),
                { ServerShutdownController {} },
                { null },
                RouteConfiguration(),
                context
            )
        }

        try {
            val cookie = "${ServerConstants.SESSION_COOKIE}=${sessionId.value}"
            val first = client.get(F95Api.CATALOG_OPTIONS) { header(HttpHeaders.Cookie, cookie) }
            val etag = first.headers[HttpHeaders.ETag]
            assertEquals(HttpStatusCode.OK, first.status)
            assertNotNull(etag)
            val second = client.get(F95Api.CATALOG_OPTIONS) {
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
