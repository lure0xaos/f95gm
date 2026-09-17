package f95gm.server.kernel

import java.io.IOException
import java.sql.SQLException
import kotlinx.coroutines.CancellationException

internal enum class FailureCategory {
    CANCELLATION,
    API,
    VALIDATION,
    UPSTREAM_TRANSPORT,
    DATABASE,
    CONFIGURATION,
    LIFECYCLE,
    CACHE,
    APPLICATION
}

internal fun Throwable.failureCategory(): FailureCategory = when (this) {
    is BoundaryFailure -> boundaryCategory
    is CancellationException -> FailureCategory.CANCELLATION
    is ApiFailureMarker -> FailureCategory.API
    is IllegalArgumentException -> FailureCategory.VALIDATION
    is IOException -> FailureCategory.UPSTREAM_TRANSPORT
    is SQLException -> FailureCategory.DATABASE
    else -> FailureCategory.APPLICATION
}
