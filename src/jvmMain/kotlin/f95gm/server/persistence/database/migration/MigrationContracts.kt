package f95gm.server.persistence.database.migration

import org.jetbrains.exposed.v1.jdbc.JdbcTransaction

internal data class DatabaseMigration(
    val version: DatabaseSchemaVersion,
    val checksum: MigrationChecksum,
    val description: String,
    val apply: JdbcTransaction.() -> Unit,
    val rollback: (JdbcTransaction.() -> Unit)?
)

internal enum class MigrationDirection { APPLY, ROLLBACK }

internal data class MigrationStep(val metadata: DatabaseMigration, val direction: MigrationDirection)

internal data class MigrationExecution(
    val fromVersion: DatabaseSchemaVersion,
    val toVersion: DatabaseSchemaVersion,
    val appliedSteps: Int
)

internal data class MigrationPlanReport(
    val fromVersion: DatabaseSchemaVersion,
    val toVersion: DatabaseSchemaVersion,
    val steps: List<MigrationStep>
) {
    val isNoOp: Boolean get() = steps.isEmpty()
}
