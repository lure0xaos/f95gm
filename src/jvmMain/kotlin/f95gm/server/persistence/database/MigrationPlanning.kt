package f95gm.server.persistence.database

import f95gm.server.persistence.database.migration.DatabaseMigration
import f95gm.server.persistence.database.migration.DatabaseSchemaVersion
import f95gm.server.persistence.database.migration.MigrationDirection
import f95gm.server.persistence.database.migration.MigrationPlanReport
import f95gm.server.persistence.database.migration.MigrationStep

internal enum class MigrationPlanningError {
    NON_CONTIGUOUS_REGISTRY,
    BLANK_CHECKSUM,
    BLANK_DESCRIPTION,
    VERSION_OUT_OF_RANGE,
    MISSING_ROLLBACK
}

internal sealed interface MigrationPlanResult {
    data class Valid(val steps: List<MigrationStep>) : MigrationPlanResult
    data class Invalid(val error: MigrationPlanningError, val message: String) : MigrationPlanResult
}

internal fun validateMigrationRegistry(registry: List<DatabaseMigration>) {
    when (val result = validateMigrationRegistryResult(registry)) {
        is MigrationPlanResult.Valid -> Unit
        is MigrationPlanResult.Invalid -> throw IllegalArgumentException(result.message)
    }
}

internal fun validateMigrationRegistryResult(registry: List<DatabaseMigration>): MigrationPlanResult = when {
    registry.map { it.version.value } != registry.indices.map { it + 1 } ->
        MigrationPlanResult.Invalid(MigrationPlanningError.NON_CONTIGUOUS_REGISTRY, "Migration registry versions must be contiguous and start at one")
    registry.any { it.checksum.value.isBlank() } ->
        MigrationPlanResult.Invalid(MigrationPlanningError.BLANK_CHECKSUM, "Migration checksums must not be blank")
    registry.any { it.description.isBlank() } ->
        MigrationPlanResult.Invalid(MigrationPlanningError.BLANK_DESCRIPTION, "Migration descriptions must not be blank")
    else -> MigrationPlanResult.Valid(emptyList())
}

internal fun migrationPlanResult(fromVersion: Int, toVersion: Int, registry: List<DatabaseMigration>): MigrationPlanResult {
    val validation = validateMigrationRegistryResult(registry)
    if (validation is MigrationPlanResult.Invalid) return validation
    if (fromVersion !in 0..registry.size || toVersion !in 0..registry.size) {
        return MigrationPlanResult.Invalid(MigrationPlanningError.VERSION_OUT_OF_RANGE, "Migration target is outside the registered schema range")
    }
    return when {
        fromVersion < toVersion -> MigrationPlanResult.Valid(
            registry.filter { it.version.value in (fromVersion + 1)..toVersion }
                .map { MigrationStep(it, MigrationDirection.APPLY) }
        )
        fromVersion > toVersion -> {
            val selected = registry.filter { it.version.value in (toVersion + 1)..fromVersion }.reversed()
            val missing = selected.firstOrNull { it.rollback == null }
            if (missing != null) MigrationPlanResult.Invalid(
                MigrationPlanningError.MISSING_ROLLBACK,
                "Migration ${missing.version} has no rollback"
            ) else MigrationPlanResult.Valid(selected.map { MigrationStep(it, MigrationDirection.ROLLBACK) })
        }
        else -> MigrationPlanResult.Valid(emptyList())
    }
}

internal fun migrationPlan(fromVersion: Int, toVersion: Int, registry: List<DatabaseMigration>): List<MigrationStep> {
    return when (val result = migrationPlanResult(fromVersion, toVersion, registry)) {
        is MigrationPlanResult.Valid -> result.steps
        is MigrationPlanResult.Invalid -> throw IllegalArgumentException(result.message)
    }
}

internal fun migrationPlanReport(fromVersion: Int, toVersion: Int, registry: List<DatabaseMigration>): MigrationPlanReport =
    MigrationPlanReport(DatabaseSchemaVersion(fromVersion), DatabaseSchemaVersion(toVersion), migrationPlan(fromVersion, toVersion, registry))
