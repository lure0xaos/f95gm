package f95gm.server.http.request.ratelimit

import f95gm.server.config.constants.ServerConstants
import io.ktor.client.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

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
                        nowMillis = 1_000
                    )
                    call.respondText("ok")
                }
            }
        }

        val response = client.get("/rate-limit")

        assertEquals("4", response.headers[ServerConstants.RATE_LIMIT_HEADER])
        assertEquals("2", response.headers[ServerConstants.RATE_LIMIT_REMAINING_HEADER])
        assertEquals("8", response.headers[ServerConstants.RATE_LIMIT_RESET_HEADER])
        assertEquals("10", response.headers[ServerConstants.RATE_LIMIT_IP_LIMIT_HEADER])
        assertEquals("7", response.headers[ServerConstants.RATE_LIMIT_IP_REMAINING_HEADER])
        assertEquals("5", response.headers[ServerConstants.RATE_LIMIT_IP_RESET_HEADER])
        assertEquals("4", response.headers[ServerConstants.RATE_LIMIT_ACCOUNT_LIMIT_HEADER])
        assertEquals("2", response.headers[ServerConstants.RATE_LIMIT_ACCOUNT_REMAINING_HEADER])
        assertEquals("8", response.headers[ServerConstants.RATE_LIMIT_ACCOUNT_RESET_HEADER])
    }

    @Test
    fun retryAfterUsesTheLatestResetAndRoundsSmallDurationsUp() {
        val decisions = listOf(
            RequestRateLimiter.Decision(false, 10, 0, 1_500),
            RequestRateLimiter.Decision(false, 4, 0, 4_500)
        )

        assertEquals("3", RateLimitHeaders.retryAfterSeconds(decisions, nowMillis = 1_500))
        assertEquals("1", RateLimitHeaders.retryAfterSeconds(decisions, nowMillis = 4_500))
    }
}
