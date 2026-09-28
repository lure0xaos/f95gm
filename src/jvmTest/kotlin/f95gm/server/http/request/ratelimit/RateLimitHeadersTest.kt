package f95gm.server.http.request.ratelimit

import io.ktor.client.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import f95gm.server.model.EpochMillis

class RateLimitHeadersTest {
    @Test
    fun appendsAggregateAndScopedHeadersForBothRateLimitScopes() = testApplication {
        application {
            routing {
                get("/rate-limit") {
                    RateLimitHeaders.append(
                        call,
                        ip = RequestRateLimiter.Decision(true, limit = 10, remaining = 7, resetAtMillis = 5_000),
                        account = RequestRateLimiter.Decision(true, limit = 4, remaining = 2, resetAtMillis = 8_000),
                        now = EpochMillis(1_000)
                    )
                    call.respondText("ok")
                }
            }
        }

        val response = client.get("/rate-limit")

        assertEquals("4", response.headers[f95gm.server.config.protocol.RateLimitProtocolConfig.LIMIT])
        assertEquals("2", response.headers[f95gm.server.config.protocol.RateLimitProtocolConfig.REMAINING])
        assertEquals("8", response.headers[f95gm.server.config.protocol.RateLimitProtocolConfig.RESET])
        assertEquals("10", response.headers[f95gm.server.config.protocol.RateLimitProtocolConfig.IP_LIMIT])
        assertEquals("7", response.headers[f95gm.server.config.protocol.RateLimitProtocolConfig.IP_REMAINING])
        assertEquals("5", response.headers[f95gm.server.config.protocol.RateLimitProtocolConfig.IP_RESET])
        assertEquals("4", response.headers[f95gm.server.config.protocol.RateLimitProtocolConfig.ACCOUNT_LIMIT])
        assertEquals("2", response.headers[f95gm.server.config.protocol.RateLimitProtocolConfig.ACCOUNT_REMAINING])
        assertEquals("8", response.headers[f95gm.server.config.protocol.RateLimitProtocolConfig.ACCOUNT_RESET])
    }

    @Test
    fun retryAfterUsesTheLatestResetAndRoundsSmallDurationsUp() {
        val decisions = listOf(
            RequestRateLimiter.Decision(false, 10, 0, 1_500),
            RequestRateLimiter.Decision(false, 4, 0, 4_500)
        )

        assertEquals("3", RateLimitHeaders.retryAfterSeconds(decisions, now = EpochMillis(1_500)))
        assertEquals("1", RateLimitHeaders.retryAfterSeconds(decisions, now = EpochMillis(4_500)))
    }
}
