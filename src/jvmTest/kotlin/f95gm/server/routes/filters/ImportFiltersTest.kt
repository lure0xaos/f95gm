package f95gm.server.routes.filters

import f95gm.domain.api.SavedFilterRequest
import f95gm.domain.values.F95OptionName
import f95gm.server.model.filters.SavedFilter
import kotlin.test.Test
import kotlin.test.assertEquals

class ImportFiltersTest {
    private fun request(name: String) = SavedFilterRequest(name = F95OptionName(name))

    @Test
    fun invalidDuplicateDoesNotHideLaterValidFilterOrConsumeLimit() {
        val imported = prepareImportedFilters(
            requests = listOf(request("same"), request("SAME"), request("other")),
            maxCount = 2
        ) { request ->
            if (request.name.value == "same") null
            else SavedFilter(name = request.name)
        }

        assertEquals(listOf("SAME", "other"), imported.map { it.name.value })
    }

    @Test
    fun duplicateValidFiltersAreCollapsedBeforeApplyingLimit() {
        val imported = prepareImportedFilters(
            requests = listOf(request(" first "), request("FIRST"), request("second")),
            maxCount = 2
        ) { request -> SavedFilter(name = request.name) }

        assertEquals(listOf(" first ", "second"), imported.map { it.name.value })
    }
}
