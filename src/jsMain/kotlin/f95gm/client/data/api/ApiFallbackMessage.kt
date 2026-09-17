package f95gm.client.data.api

import f95gm.messages.UiMessages
import f95gm.client.model.runtime.HttpStatusCodeValue

/** Typed fallback selection for transport failures; status formatting stays in one boundary. */
internal enum class ApiFallbackMessage {
    REQUEST_FAILED,
    CATALOG_REQUEST_FAILED
}

internal fun interface ApiStatusFallback {
    fun resolve(status: HttpStatusCodeValue): String
}

internal fun ApiFallbackMessage.resolve(status: HttpStatusCodeValue): String = when (this) {
    ApiFallbackMessage.REQUEST_FAILED -> UiMessages.app_requestFailed(status.value)
    ApiFallbackMessage.CATALOG_REQUEST_FAILED -> UiMessages.app_catalogRequestFailed(status.value)
}
