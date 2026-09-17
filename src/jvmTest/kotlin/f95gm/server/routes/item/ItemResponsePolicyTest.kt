package f95gm.server.routes.item

import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals

class ItemResponsePolicyTest {
    @Test
    fun translatesForbiddenAndMissingUpstreamStatusesToSpecificMessages() {
        assertEquals(itemFailureMessage(HttpStatusCode.Forbidden), itemFailureResponse(HttpStatusCode.Forbidden).failure.message)
        assertEquals(itemFailureMessage(HttpStatusCode.NotFound), itemFailureResponse(HttpStatusCode.NotFound).failure.message)
        assertEquals(itemFailureMessage(HttpStatusCode.BadGateway), itemFailureResponse(HttpStatusCode.BadGateway).failure.message)
        assertEquals(HttpStatusCode.Forbidden, itemFailureResponse(HttpStatusCode.Forbidden).status)
        assertEquals(HttpStatusCode.NotFound, itemFailureResponse(HttpStatusCode.NotFound).status)
    }
}
