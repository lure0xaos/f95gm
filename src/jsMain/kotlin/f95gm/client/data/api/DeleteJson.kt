package f95gm.client.data.api

import f95gm.client.state.runtime.http
import f95gm.messages.UiMessages
import io.ktor.http.*

internal suspend fun deleteJson(path: String, transport: ApiTransport = ApiTransport(http)): String = requestJson(
    path = path,
    method = HttpMethod.Delete,
    fallback = { status -> UiMessages.app_requestFailed(status) },
    transport = transport
)
