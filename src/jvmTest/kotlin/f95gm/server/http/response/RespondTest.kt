package f95gm.server.http.response

import f95gm.domain.api.F95ApiError
import f95gm.server.state.runtime.IdempotencyRegistry
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RespondTest {
    @Test
    fun cacheableJsonReturns304ForAWeakMatchingValidator() = testApplication {
        application {
            routing {
                get("/cacheable") {
                    respondJsonCacheable(call, HttpStatusCode.OK, F95ApiError("cached"), maxAgeSeconds = -1)
                }
            }
        }

        val first = client.get("/cacheable")
        val etag = assertNotNull(first.headers[HttpHeaders.ETag])
        val second = client.get("/cacheable") {
            header(HttpHeaders.IfNoneMatch, "W/$etag")
        }

        assertEquals(HttpStatusCode.OK, first.status)
        assertEquals("private, max-age=0", first.headers[HttpHeaders.CacheControl])
        assertEquals(HttpStatusCode.NotModified, second.status)
        assertEquals(etag, second.headers[HttpHeaders.ETag])
    }

    @Test
    fun cachedResponsesReplayStatusBodyAndContentType() = testApplication {
        application {
            routing {
                get("/cached") {
                    respondCached(
                        this.call,
                        IdempotencyRegistry.CachedResponse(
                            status = HttpStatusCode.Accepted.value,
                            body = "accepted",
                            contentType = ContentType.Text.Plain.toString()
                        )
                    )
                }
            }
        }

        val response = client.get("/cached")

        assertEquals(HttpStatusCode.Accepted, response.status)
        assertEquals("accepted", response.bodyAsText())
        assertEquals(ContentType.Text.Plain.toString(), response.headers[HttpHeaders.ContentType]?.substringBefore(';'))
    }
}
