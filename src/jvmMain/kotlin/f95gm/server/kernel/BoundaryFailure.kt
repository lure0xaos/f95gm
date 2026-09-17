package f95gm.server.kernel

/** Common root for failures raised at infrastructure and lifecycle boundaries. */
internal open class BoundaryFailure(
    message: String,
    cause: Throwable? = null,
    internal val boundaryCategory: FailureCategory = FailureCategory.APPLICATION
) : IllegalStateException(message, cause)
