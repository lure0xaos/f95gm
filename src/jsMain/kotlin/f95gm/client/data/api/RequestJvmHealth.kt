package f95gm.client.data.api

import f95gm.client.data.api.ApiClientEvents
import f95gm.domain.api.ApiPath
import f95gm.domain.api.F95ApiPaths
import f95gm.domain.api.ApiBrowserTabId
import f95gm.domain.api.ApiProbeNumber
import f95gm.domain.api.ApiTheme

internal suspend fun requestJvmHealth(
    probe: Int,
    tabId: String,
    theme: String,
    transport: ApiTransport,
    events: ApiClientEvents
): Int = apiRequest(ApiPath(F95ApiPaths.HEALTH), transport = transport, events = events, request = { transport ->
        transport.request(F95ApiPaths.healthPath(ApiProbeNumber(probe), ApiBrowserTabId(tabId), ApiTheme(theme))) {}
    }).status.value
