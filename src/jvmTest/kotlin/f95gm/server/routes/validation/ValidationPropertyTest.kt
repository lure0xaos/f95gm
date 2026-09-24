package f95gm.server.routes.validation

import f95gm.server.config.constants.ServerConstants
import f95gm.server.routes.catalog.isNumericId
import f95gm.server.routes.filters.validation.isSavedFilterAlias
import f95gm.server.routes.filters.validation.isValidSavedFilterId
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidationPropertyTest {
    @Test
    fun numericIdsAcceptExactlyPositiveDecimalIntegers() {
        for (value in 1..10_000) assertTrue(isNumericId(value.toString()))
        val random = Random(42)
        repeat(1_000) {
            val candidate = buildString {
                repeat(random.nextInt(1, 12)) { append(random.nextInt(32, 127).toChar()) }
            }
            val expected = candidate.toIntOrNull()?.let { it > 0 } == true
            assertTrue(isNumericId(candidate) == expected, candidate)
        }
        assertFalse(isNumericId("0"))
        assertFalse(isNumericId("-1"))
        assertFalse(isNumericId("01x"))
    }

    @Test
    fun savedFilterAliasesAndIdsRespectTheirCharacterAndLengthContracts() {
        val validAliases = listOf("favorites", "rpg-2026", "a1-b2-c3")
        validAliases.forEach { alias ->
            assertTrue(isSavedFilterAlias(alias))
            assertTrue(isValidSavedFilterId(alias))
        }
        val invalidAliases = listOf("", "-start", "end-", "double--dash", "Upper", "with_under")
        invalidAliases.forEach { assertFalse(isSavedFilterAlias(it), it) }
        assertFalse(isValidSavedFilterId("a".repeat(ServerConstants.FILTER_ID_LENGTH + 1)))
        assertTrue(isValidSavedFilterId("00000000-0000-0000-0000-000000000001"))
    }
}
