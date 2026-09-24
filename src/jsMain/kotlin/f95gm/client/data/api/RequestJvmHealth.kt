package f95gm.client.data.api

import f95gm.client.state.runtime.http
import f95gm.domain.api.F95Api

internal suspend fun requestJvmHealth(probe: Int, tabId: String, theme: String): Int =
    apiRequest(F95Api.HEALTH, transport = ApiTransport(http), request = { transport ->
        transport.request("${F95Api.HEALTH}?probe=$probe&${F95Api.TAB_ID}=$tabId&${F95Api.THEME}=$theme") {}
    }).status.value
