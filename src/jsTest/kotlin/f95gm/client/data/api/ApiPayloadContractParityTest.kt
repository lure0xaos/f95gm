package f95gm.client.data.api

import f95gm.client.state.runtime.clientJson
import f95gm.domain.api.MarkResponseContract
import f95gm.domain.api.SavedFilterContract
import f95gm.domain.api.ThreadDetailContract
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals

/** Keeps tolerant client payload models aligned with stable API response shapes. */
class ApiPayloadContractParityTest {
    @Test
    fun markPayloadAndStableContractShareTheSameResponseFields() {
        val json = """[{"threadId":"42","developer":"Studio","updatedAt":7}]"""

        val payload = clientJson.decodeFromString<List<MarkPayload>>(json).single()
        val contract = clientJson.decodeFromString<List<MarkResponseContract>>(json).single()

        assertEquals(contract.threadId, payload.threadId)
        assertEquals(contract.developer, payload.developer)
        assertEquals(contract.updatedAt.value, payload.updatedAt)
    }

    @Test
    fun detailPayloadAndStableContractShareTheSameResponseFields() {
        val json = """{"threadId":"42","title":"Demo","meta":[],"sections":[]}"""

        val payload = clientJson.decodeFromString<DetailPayload>(json)
        val contract = clientJson.decodeFromString<ThreadDetailContract>(json)

        assertEquals(contract.threadId, payload.threadId)
        assertEquals(contract.title, payload.title)
        assertEquals(contract.bodyHtml, payload.bodyHtml)
    }

    @Test
    fun savedFilterPayloadAndStableContractShareTheSameResponseFields() {
        val json = """[{"id":"filter-1","name":"Demo","updatedAt":9}]"""

        val payload = clientJson.decodeFromString<List<SavedFilterPayload>>(json).single()
        val contract = clientJson.decodeFromString<List<SavedFilterContract>>(json).single()

        assertEquals(contract.id, payload.id)
        assertEquals(contract.name, payload.name)
        assertEquals(contract.updatedAt.value, payload.updatedAt)
    }
}
