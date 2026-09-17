package f95gm.server.routes.filters

import f95gm.domain.api.SavedFilterRequest
import f95gm.domain.values.F95FilterId
import f95gm.domain.values.F95OptionName
import f95gm.server.model.filters.SavedFilter
import kotlin.test.Test
import kotlin.test.assertEquals

class ImportFiltersTest {
    @Test
    fun convertsDeduplicatesCaseInsensitivelyAndHonorsLimit() {
        val requests = listOf("Games", "games", "Mods", "Tools").map { SavedFilterRequest(name = F95OptionName(it)) }
        val imported = prepareImportedFilters(requests, maxCount = 2) { request ->
            SavedFilter(
                id = F95FilterId(request.name.value.lowercase()),
                name = request.name,
                updatedAt = f95gm.server.model.EpochMillis(1)
            )
        }

        assertEquals(listOf("Games", "Mods"), imported.map { it.name.value })
    }

    @Test
    fun dropsInvalidRequestsAndAcceptsEmptyImport() {
        val requests = listOf(SavedFilterRequest(name = F95OptionName("bad")), SavedFilterRequest(name = F95OptionName("good")))
        val imported = prepareImportedFilters(requests, maxCount = 10) { request ->
            request.takeIf { it.name.value == "good" }?.let {
                SavedFilter(id = F95FilterId("good"), name = it.name)
            }
        }

        assertEquals(listOf("good"), imported.map { it.id.value })
        assertEquals(emptyList(), prepareImportedFilters(emptyList(), 10) { null })
    }
}
