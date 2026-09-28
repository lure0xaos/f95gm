package f95gm.server.app

import f95gm.server.config.Bytes
import f95gm.server.config.Keys

internal data class RequestOperationalPolicy(
    val maxRequestBodyBytes: Bytes,
    val maxRateLimitBuckets: RateLimitBucketLimit
)

internal object ServerOperationalDefaults {
    val request = RequestOperationalPolicy(
        maxRequestBodyBytes = Keys.MAX_REQUEST_BODY.default,
        maxRateLimitBuckets = RateLimitBucketLimit(Keys.MAX_RATE_LIMIT_BUCKETS.default)
    )
}
