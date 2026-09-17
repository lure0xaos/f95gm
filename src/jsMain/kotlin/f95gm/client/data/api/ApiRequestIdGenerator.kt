package f95gm.client.data.api

import f95gm.client.model.runtime.ApiRequestId

internal fun interface ApiRequestIdGenerator {
    fun next(): ApiRequestId
}

internal class SequentialApiRequestIdGenerator(startAt: Long = 1L) : ApiRequestIdGenerator {
    init { require(startAt in 1 until Long.MAX_VALUE) }

    private var nextValue = startAt

    override fun next(): ApiRequestId {
        check(nextValue < Long.MAX_VALUE) { "API request ID space exhausted" }
        return ApiRequestId(nextValue++)
    }
}
