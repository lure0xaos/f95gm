package f95gm.client.data.api

import f95gm.client.state.runtime.clientJson
import f95gm.domain.api.ApiResponseEnvelope
import f95gm.domain.api.ApiStatusCode
import f95gm.domain.api.LoginRequestContract
import f95gm.domain.api.MarkResponseContract
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals

class TypedApiContractTest {
    @Test
    fun generatedDtosRoundTripThroughTheClientJsonConfiguration() {
        val request = LoginRequestContract("alice", "secret")
        assertEquals(request, clientJson.decodeFromString<LoginRequestContract>(clientJson.encodeToString(request)))
    }

    @Test
    fun typedEnvelopePreservesStatusWhileDecodingTheBody() {
        val envelope = ApiResponseEnvelope(ApiStatusCode(200), "{\"threadId\":\"42\"}")
            .mapBody { clientJson.decodeFromString<MarkResponseContract>(it) }
        assertEquals(200, envelope.status)
        assertEquals("42", envelope.body.threadId)
    }
}
