package f95gm.server.http.request

import io.ktor.server.application.*
import io.ktor.util.*
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.asContextElement
import org.slf4j.MDC
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

internal data class RequestContext(
    val requestId: RequestId,
    val remoteHost: String
)

private val requestContextKey = AttributeKey<RequestContext>("f95gm.requestContext")
private val requestContextThreadLocal = ThreadLocal<RequestContext?>()

internal fun ApplicationCall.installRequestContext(context: RequestContext) {
    attributes.put(requestContextKey, context)
}

internal val ApplicationCall.requestContext: RequestContext
    get() = attributes[requestContextKey]

internal fun RequestContext.asCoroutineContext() =
    requestContextThreadLocal.asContextElement(this) + RequestMdcContext(requestId.value)

internal fun currentRequestContext(): RequestContext? = requestContextThreadLocal.get()

private class RequestMdcContext(private val requestId: String) :
    ThreadContextElement<Map<String, String>?>,
    AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<RequestMdcContext>

    override fun updateThreadContext(context: CoroutineContext): Map<String, String>? {
        val previous = MDC.getCopyOfContextMap()
        MDC.put("requestId", requestId)
        return previous
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: Map<String, String>?) {
        MDC.clear()
        oldState?.forEach { (key, value) -> MDC.put(key, value) }
    }
}
