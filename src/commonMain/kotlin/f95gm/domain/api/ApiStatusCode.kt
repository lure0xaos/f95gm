package f95gm.domain.api

import kotlin.jvm.JvmInline

/** Valid HTTP status code carried across the shared API boundary. */
@JvmInline
value class ApiStatusCode(val value: Int) {
    init { require(value in 100..599) { "HTTP status code must be between 100 and 599" } }
}
