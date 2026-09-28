package f95gm.server.cache

import f95gm.server.observability.CacheEvictionEvent
import f95gm.server.kernel.FailureBoundary
import f95gm.server.observability.FailureReporter
import f95gm.server.observability.FailureScopes
import f95gm.server.observability.CacheFailureNamespace
import f95gm.server.observability.logger
import f95gm.server.observability.structuredLog
import f95gm.server.kernel.failureCategory

internal fun interface CacheEventSink {
    fun publish(event: CacheEvent)
}

internal object CacheFailurePolicy {
    fun report(namespace: CacheNamespace, error: Throwable) {
        FailureReporter.report(
            FailureScopes.cache(CacheFailureNamespace(namespace.value)),
            f95gm.server.observability.FailureDescriptor(FailureBoundary.CACHE, error.failureCategory()),
            error
        )
    }
}

internal object CacheLoggingEventSink : CacheEventSink {
    override fun publish(event: CacheEvent) {
        if (event is CacheEvent.Evicted && !event.expired) {
            logger.debug { structuredLog(CacheEvictionEvent()) }
        }
    }
}
