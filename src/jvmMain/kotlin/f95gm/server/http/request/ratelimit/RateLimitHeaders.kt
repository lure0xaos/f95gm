package f95gm.server.http.request.ratelimit

import f95gm.server.config.constants.ServerConstants
import io.ktor.server.application.*

internal object RateLimitHeaders {
    private data class HeaderNames(val limit: String, val remaining: String, val reset: String)

    private val ipHeaders = HeaderNames(
        ServerConstants.RATE_LIMIT_IP_LIMIT_HEADER,
        ServerConstants.RATE_LIMIT_IP_REMAINING_HEADER,
        ServerConstants.RATE_LIMIT_IP_RESET_HEADER
    )
    private val accountHeaders = HeaderNames(
        ServerConstants.RATE_LIMIT_ACCOUNT_LIMIT_HEADER,
        ServerConstants.RATE_LIMIT_ACCOUNT_REMAINING_HEADER,
        ServerConstants.RATE_LIMIT_ACCOUNT_RESET_HEADER
    )

    fun append(
        call: ApplicationCall,
        ip: RequestRateLimiter.Decision?,
        account: RequestRateLimiter.Decision?,
        nowMillis: Long
    ) {
        appendScoped(call, ip, ipHeaders, nowMillis)
        appendScoped(call, account, accountHeaders, nowMillis)
        val decisions = listOfNotNull(ip, account)
        if (decisions.isEmpty()) return
        call.response.headers.append(ServerConstants.RATE_LIMIT_HEADER, decisions.minOf { it.limit }.toString())
        call.response.headers.append(
            ServerConstants.RATE_LIMIT_REMAINING_HEADER,
            decisions.minOf { it.remaining }.toString()
        )
        call.response.headers.append(
            ServerConstants.RATE_LIMIT_RESET_HEADER,
            resetSeconds(decisions.maxOf { it.resetAtMillis }, nowMillis)
        )
    }

    fun retryAfterSeconds(decisions: Collection<RequestRateLimiter.Decision>, nowMillis: Long): String =
        decisions.maxOf { it.resetAtMillis }
            .minus(nowMillis).coerceAtLeast(0L)
            .div(ServerConstants.MILLIS_PER_SECOND).coerceAtLeast(1).toString()

    private fun appendScoped(
        call: ApplicationCall,
        decision: RequestRateLimiter.Decision?,
        headers: HeaderNames,
        nowMillis: Long
    ) {
        if (decision == null) return
        call.response.headers.append(headers.limit, decision.limit.toString())
        call.response.headers.append(headers.remaining, decision.remaining.toString())
        call.response.headers.append(headers.reset, resetSeconds(decision.resetAtMillis, nowMillis))
    }

    private fun resetSeconds(resetAtMillis: Long, nowMillis: Long): String =
        resetAtMillis.coerceAtLeast(nowMillis).div(ServerConstants.MILLIS_PER_SECOND).toString()
}
