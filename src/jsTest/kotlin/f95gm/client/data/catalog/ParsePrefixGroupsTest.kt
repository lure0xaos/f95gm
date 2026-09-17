package f95gm.client.data.catalog

import f95gm.messages.UiMessages
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class ParsePrefixGroupsTest {
    @Test
    fun supportsObjectWrappedGroupsAndAlternateChildKeys() {
        val root = Json.parseToJsonElement(
            """
                {"groups":[
                  {"group_id":"g1","name":"First","prefixes":[{"id":"1","name":"One"}]},
                  {"title":"Second","items":[{"id":"2","name":"Two"}]}
                ]}
            """.trimIndent()
        ).jsonObject

        val groups = parsePrefixGroups(root)

        assertEquals(listOf("First", "Second"), groups.map { it.name.value })
        assertEquals(listOf("g1", "1:Second"), groups.map { it.id.value })
    }

    @Test
    fun createsFallbackGroupForFlatObjectsAndDropsEmptyGroups() {
        val element = Json.parseToJsonElement(
            """
                [{"id":"1","title":"One"},{"id":"2"},{"id":"3","name":"Three"},{}]
            """.trimIndent()
        )

        val groups = parsePrefixGroups(element)

        assertEquals(listOf(UiMessages.app_prefixes()), groups.map { it.name.value })
        assertEquals(listOf("One", "Three"), groups.single().options.map { it.name.value })
    }
}
