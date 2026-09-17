package f95gm.server.http.request.ratelimit

import f95gm.server.config.protocol.RateLimitProtocolConfig
import io.ktor.server.application.*
import f95gm.server.model.EpochMillis

internal object RateLimitHeaders {
    private data class HeaderNames(val limit: String, val remaining: String, val reset: String)

    private val ipHeaders = HeaderNames(
        RateLimitProtocolConfig.IP_LIMIT,
        RateLimitProtocolConfig.IP_REMAINING,
        RateLimitProtocolConfig.IP_RESET
    )
    private val accountHeaders = HeaderNames(
        RateLimitProtocolConfig.ACCOUNT_LIMIT,
        RateLimitProtocolConfig.ACCOUNT_REMAINING,
        RateLimitProtocolConfig.ACCOUNT_RESET
    )

    fun append(
        call: ApplicationCall,
        ip: RequestRateLimiter.Decision?,
        account: RequestRateLimiter.Decision?,
        now: EpochMillis
    ) {
        appendScoped(call, ip, ipHeaders, now)
        appendScoped(call, account, accountHeaders, now)
        val decisions = listOfNotNull(ip, account)
        if (decisions.isEmpty()) return
        call.response.headers.append(RateLimitProtocolConfig.LIMIT, decisions.minOf { it.limit }.toString())
        call.response.headers.append(
            RateLimitProtocolConfig.REMAINING,
            decisions.minOf { it.remaining }.toString()
        )
        call.response.headers.append(
            RateLimitProtocolConfig.RESET,
            resetSeconds(decisions.maxOf { it.resetAtMillis }, now.value)
        )
    }

    fun retryAfterSeconds(decisions: Collection<RequestRateLimiter.Decision>, now: EpochMillis): String =
        decisions.maxOf { it.resetAtMillis }
            .minus(now.value).coerceAtLeast(0L)
            .div(1000L).coerceAtLeast(1).toString()

    private fun appendScoped(
        call: ApplicationCall,
        decision: RequestRateLimiter.Decision?,
        headers: HeaderNames,
        now: EpochMillis
    ) {
        if (decision == null) return
        call.response.headers.append(headers.limit, decision.limit.toString())
        call.response.headers.append(headers.remaining, decision.remaining.toString())
        call.response.headers.append(headers.reset, resetSeconds(decision.resetAtMillis, now.value))
    }

    private fun resetSeconds(resetAtMillis: Long, nowMillis: Long): String =
        resetAtMillis.coerceAtLeast(nowMillis).div(1000L).toString()
}
