package f95gm.server.upstream.request

import java.io.IOException

/** Maps transport exceptions to the single upstream outcome contract. */
internal object UpstreamTransportFailureAdapter {
    fun adapt(error: Throwable): UpstreamResponseDisposition? = when (error) {
        is UpstreamRequestDeadlineExceeded -> UpstreamFailurePolicy.timeout()
        is IOException -> UpstreamFailurePolicy.transport()
        else -> null
    }
}
