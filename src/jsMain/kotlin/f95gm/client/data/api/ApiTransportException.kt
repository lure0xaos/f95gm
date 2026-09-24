package f95gm.client.data.api

internal class ApiTransportException(path: String, cause: Throwable) :
    RuntimeException("Transport request failed for $path", cause)
