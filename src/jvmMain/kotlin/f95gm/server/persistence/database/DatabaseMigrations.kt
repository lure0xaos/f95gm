package f95gm.server.persistence.database

import f95gm.server.persistence.schema.SchemaVersionTable
import f95gm.server.persistence.database.migration.*
import f95gm.server.persistence.database.migration.v1InitialSchemaMigration
import f95gm.server.persistence.database.migration.V1_SCHEMA_CHECKSUM
import f95gm.server.kernel.FailureBoundary
import f95gm.server.observability.FailureReporter
import f95gm.server.observability.FailureScope
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

internal const val CURRENT_SCHEMA_VERSION = 1
internal const val CURRENT_SCHEMA_CHECKSUM = V1_SCHEMA_CHECKSUM

internal val migrationRegistry = listOf(
    v1InitialSchemaMigration()
)

/** Executes a registered rollback as one transaction, including the version marker update. */
internal fun rollbackDatabase(database: Database, targetVersion: Int, afterStep: ((MigrationStep) -> Unit)? = null): MigrationExecution {
    require(targetVersion in 0 until CURRENT_SCHEMA_VERSION) { "Rollback target is outside the supported range" }
    return MigrationLock.withLock {
        FailureReporter.runBoundary(
            scope = FailureScope("database-rollback"),
            boundary = FailureBoundary.PERSISTENCE,
            transform = ::DatabaseMigrationFailure,
            onFailure = { restoreCurrentSchemaAfterRollbackFailure(database) },
            block = {
            transaction(database) {
                val current = SchemaVersionTable.selectAll().singleOrNull()?.get(SchemaVersionTable.version) ?: 0
                val plan = migrationPlan(current, targetVersion, migrationRegistry)
                val executor = DatabaseMigrationExecutor(migrationRegistry)
                plan.forEach { step ->
                    when (val result = executor.rollbackResult(this, step)) {
                        DatabaseMigrationExecutor.MigrationStepResult.RolledBack -> Unit
                        is DatabaseMigrationExecutor.MigrationStepResult.WrongDirection ->
                            error("Migration step must have direction ${result.expected}")
                        DatabaseMigrationExecutor.MigrationStepResult.MissingRollback ->
                            error("No rollback handler registered for migration ${step.metadata.version}")
                        else -> error("Unexpected migration result: $result")
                    }
                    afterStep?.invoke(step)
                }
                SchemaVersionTable.update({ SchemaVersionTable.id eq 1 }) { it[version] = targetVersion }
                MigrationExecution(DatabaseSchemaVersion(current), DatabaseSchemaVersion(targetVersion), plan.size)
            }
            }
        )
    }
}

private fun restoreCurrentSchemaAfterRollbackFailure(database: Database) {
    transaction(database) {
        ensureSchemaTables()
        restoreCurrentSchemaMetadata()
    }
}

/** Compatibility is retained until the next schema generation is released. */
internal object MigrationCleanupPolicy {
    const val LEGACY_FIXTURE_VERSION = 0
    const val REMOVE_LEGACY_FIXTURES_AFTER_VERSION = 2

    fun retains(version: Int): Boolean = version < REMOVE_LEGACY_FIXTURES_AFTER_VERSION
}

internal fun migrateDatabase(database: Database): MigrationExecution {
    val executor = DatabaseMigrationExecutor(migrationRegistry)
    return MigrationLock.withLock {
        transaction(database) {
            exec("CREATE TABLE IF NOT EXISTS schema_migration_lock (id INT PRIMARY KEY)")
            exec("MERGE INTO schema_migration_lock (id) KEY(id) VALUES (1)")
            exec("SELECT id FROM schema_migration_lock WHERE id = 1 FOR UPDATE")

            ensureSchemaTables()
            val current = SchemaVersionTable.selectAll().singleOrNull()?.get(SchemaVersionTable.version) ?: 0
            require(current <= CURRENT_SCHEMA_VERSION) { "Unsupported database schema version: $current" }
            when (val result = executor.verifyChecksumsResult(this, current)) {
                DatabaseMigrationExecutor.ChecksumVerificationResult.Verified -> Unit
                is DatabaseMigrationExecutor.ChecksumVerificationResult.Mismatch ->
                    throw IllegalArgumentException(
                        "Database migration checksum mismatch for schema version ${result.version}"
                    )
            }
            val plan = migrationPlan(current, CURRENT_SCHEMA_VERSION, migrationRegistry)
            plan.forEach { step ->
                when (val result = executor.applyResult(this, step)) {
                    DatabaseMigrationExecutor.MigrationStepResult.Applied -> Unit
                    is DatabaseMigrationExecutor.MigrationStepResult.WrongDirection ->
                        error("Migration step must have direction ${result.expected}")
                    else -> error("Unexpected migration result: $result")
                }
            }
            executor.recordCurrentVersion(this)
            MigrationExecution(DatabaseSchemaVersion(current), DatabaseSchemaVersion(CURRENT_SCHEMA_VERSION), plan.size)
        }
    }
}
