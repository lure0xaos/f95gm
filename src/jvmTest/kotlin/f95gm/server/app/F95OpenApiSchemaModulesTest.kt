package f95gm.server.app
import f95gm.domain.api.*

import f95gm.domain.api.OpenApiSchemaModule
import f95gm.domain.api.schemaModuleInventory
import f95gm.domain.api.validateSchemaModuleInventory
import f95gm.domain.api.openApiContractJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class F95OpenApiSchemaModulesTest {
    @Test
    fun everySchemaBelongsToExactlyOneOpenApiModule() {
        val inventory = schemaModuleInventory()
        validateSchemaModuleInventory(inventory)
        assertEquals(ApiSchema.entries.toSet(), inventory.values.flatten().toSet())
        assertEquals(ApiSchema.entries.size, inventory.values.sumOf { it.size })
    }

    @Test
    fun schemaModuleValidatorRejectsMutationThatDropsASchema() {
        val inventory = schemaModuleInventory().toMutableMap()
        inventory[OpenApiSchemaModule.OPERATIONAL] =
            inventory.getValue(OpenApiSchemaModule.OPERATIONAL) - ApiSchema.API_ERROR
        assertFailsWith<IllegalArgumentException> { validateSchemaModuleInventory(inventory) }
    }

    @Test
    fun requiredSchemaFieldsRemainDeclaredInTheGeneratedContract() {
        val contract = openApiContractJson()
        assertEquals(true, contract.contains("\"required\""))
        assertEquals(true, contract.contains("\"login\""))
        assertEquals(true, contract.contains("\"trackingState\""))
    }
}
