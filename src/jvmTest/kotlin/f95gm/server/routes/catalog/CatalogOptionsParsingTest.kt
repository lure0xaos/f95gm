package f95gm.server.routes.catalog

import f95gm.server.routes.catalog.parsing.extractJavaScriptObject
import f95gm.server.routes.catalog.parsing.findJavaScriptAssignment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CatalogOptionsParsingTest {
    @Test
    fun findsStandaloneDeclaredAndWindowAssignmentsWithoutMatchingIdentifiersOrProperties() {
        assertEquals(12, findJavaScriptAssignment("const tags = {}", "tags"))
        assertEquals(13, findJavaScriptAssignment("window.tags = {}", "tags"))
        assertNull(findJavaScriptAssignment("var latestUpdatesExtra = {}", "latestUpdates"))
        assertNull(findJavaScriptAssignment("object.latestUpdates = {}", "latestUpdates"))
        assertNull(findJavaScriptAssignment("latestUpdates == {}", "latestUpdates"))
    }

    @Test
    fun extractsBalancedObjectWhileIgnoringBracesInsideQuotedValues() {
        assertEquals(
            "{\"tags\":{\"label\":\"}\"},\"prefixes\":{}}",
            extractJavaScriptObject(
                "<script>window.latestUpdates = {\"tags\":{\"label\":\"}\"},\"prefixes\":{}}; after()</script>",
                "latestUpdates"
            )
        )
        assertNull(extractJavaScriptObject("<script>var latestUpdates = {\"broken\": 1</script>", "latestUpdates"))
        assertNull(extractJavaScriptObject("<div>latestUpdates = {}</div>", "latestUpdates"))
    }
}
