package f95gm.server.http.request

import f95gm.server.kernel.ClockSnapshot
import io.ktor.server.application.*
import io.ktor.util.*
import kotlinx.coroutines.ThreadContextElement
import org.slf4j.MDC
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

internal data class RequestContext(
    val requestId: RequestId,
    val remoteHost: String
)

internal data class RequestCorrelationContext(val requestId: RequestId) :
    AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<RequestCorrelationContext>
}

internal data class RequestScope(
    val context: RequestContext,
    val clockSnapshot: ClockSnapshot,
    val metrics: RequestMetrics
)

/** Typed MDC snapshot prevents request correlation from depending on arbitrary map keys. */
private data class MdcSnapshot(val values: Map<String, String>?)

private val requestContextKey = AttributeKey<RequestContext>("f95gm.requestContext")
private val requestScopeKey = AttributeKey<RequestScope>("f95gm.requestScope")
private object RequestMdcKeys { const val REQUEST_ID = "requestId" }
internal fun ApplicationCall.installRequestContext(context: RequestContext) {
    attributes.put(requestContextKey, context)
}

internal fun ApplicationCall.installRequestScope(scope: RequestScope) {
    attributes.put(requestScopeKey, scope)
}

internal val ApplicationCall.requestScope: RequestScope
    get() = attributes[requestScopeKey]

internal val ApplicationCall.requestContext: RequestContext
    get() = attributes[requestContextKey]

internal fun RequestContext.asCoroutineContext(): CoroutineContext = RequestCorrelationContext(requestId)
    .plus(RequestMdcContext(requestId.value))

internal fun currentRequestId(): RequestId? = RequestId.parseOrNull(MDC.get(RequestMdcKeys.REQUEST_ID))
internal fun currentRequestIdValue(): String? = currentRequestId()?.value
internal fun currentCorrelationIdValue(): String? = currentRequestIdValue()

private class RequestMdcContext(private val requestId: String) :
    ThreadContextElement<MdcSnapshot>,
    AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<RequestMdcContext>

    override fun updateThreadContext(context: CoroutineContext): MdcSnapshot {
        val previous = MDC.getCopyOfContextMap()
        MDC.put(RequestMdcKeys.REQUEST_ID, requestId)
        return MdcSnapshot(previous)
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: MdcSnapshot) {
        MDC.clear()
        oldState.values?.forEach { (key, value) -> MDC.put(key, value) }
    }
}
