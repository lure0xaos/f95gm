package f95gm.client.data.api

import f95gm.domain.api.ApiPath

internal class ApiTransportException(path: ApiPath, cause: Throwable) :
    RuntimeException("Transport request failed for $path", cause)
