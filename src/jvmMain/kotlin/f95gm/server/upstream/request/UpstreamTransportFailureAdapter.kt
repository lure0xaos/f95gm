package f95gm.server.upstream.request

import kotlinx.coroutines.TimeoutCancellationException
import java.io.IOException

/** Maps transport exceptions to the single upstream outcome contract. */
internal object UpstreamTransportFailureAdapter {
    fun adapt(error: Throwable): UpstreamResponseDisposition? = when (error) {
        is IOException -> UpstreamFailurePolicy.transport()
        is TimeoutCancellationException -> UpstreamFailurePolicy.timeout()
        else -> null
    }
}
