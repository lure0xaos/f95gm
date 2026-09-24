package f95gm.client.data.catalog

import f95gm.messages.UiMessages
import kotlin.test.Test
import kotlin.test.assertEquals

class ParseCatalogOptionsTest {
    @Test
    fun sortsNonBlankTagsAndNestedPrefixGroups() {
        val options = parseCatalogOptions(
            """
                {
                  "tags":{"2":"zeta","1":"Alpha","0":" "},
                  "prefixes":[
                    {"id":"g1","name":"Genre","children":[{"id":"2","name":"Zoo"},{"id":"1","name":"Action"}]},
                    {"id":"g2","title":"Other","items":[{"id":"3","title":"Beta"}]}
                  ]
                }
            """.trimIndent()
        )

        assertEquals(listOf("Alpha", "zeta"), options.tags.map { it.name.value })
        assertEquals(listOf("Action", "Beta", "Zoo"), options.prefixes.map { it.name.value })
        assertEquals(listOf("Genre", "Other"), options.prefixGroups.map { it.name.value })
    }

    @Test
    fun supportsFlatPrefixArraysAndIgnoresMalformedEntries() {
        val options = parseCatalogOptions(
            """
                {
                  "prefixes":[{"id":"1","name":"One"},{"id":"1","name":"Duplicate"},"invalid",{"id":"2"}]
                }
            """.trimIndent()
        )

        assertEquals(listOf("One"), options.prefixes.map { it.name.value })
        assertEquals(listOf(UiMessages.app_prefixes()), options.prefixGroups.map { it.name.value })
    }
}
