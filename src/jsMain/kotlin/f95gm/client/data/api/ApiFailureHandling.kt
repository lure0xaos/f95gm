package f95gm.client.data.api

internal fun Throwable.apiFailureKind(): ApiFailureKind =
    (this as? ApiException)?.kind ?: ApiFailureKind.INTERNAL
