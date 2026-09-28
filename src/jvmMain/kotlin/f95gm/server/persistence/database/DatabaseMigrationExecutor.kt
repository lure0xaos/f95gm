package f95gm.server.persistence.database

import f95gm.server.persistence.schema.SchemaMigrationChecksumTable
import f95gm.server.persistence.schema.SchemaVersionTable
import f95gm.server.persistence.database.migration.*
import f95gm.server.kernel.ServerInvariantViolation
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

/** Executes migration steps against an injected registry, keeping step behavior independently testable. */
internal class DatabaseMigrationExecutor(
    private val registry: List<DatabaseMigration>
) {
    init { validateMigrationRegistry(registry) }

    internal sealed interface ChecksumVerificationResult {
        data object Verified : ChecksumVerificationResult
        data class Mismatch(val version: Int) : ChecksumVerificationResult
    }

    internal sealed interface MigrationStepResult {
        data object Applied : MigrationStepResult
        data object RolledBack : MigrationStepResult
        data class WrongDirection(val expected: MigrationDirection) : MigrationStepResult
        data object MissingRollback : MigrationStepResult
    }

    fun verifyChecksumsResult(transaction: JdbcTransaction, currentVersion: Int): ChecksumVerificationResult {
        registry.filter { it.version.value <= currentVersion }.forEach { migration ->
            val recordedChecksum = with(transaction) {
                SchemaMigrationChecksumTable.selectAll()
                    .where { SchemaMigrationChecksumTable.version eq migration.version.value }
                    .singleOrNull()
                    ?.get(SchemaMigrationChecksumTable.checksum)
            }
            if (recordedChecksum != null && recordedChecksum != migration.checksum.value) {
                return ChecksumVerificationResult.Mismatch(migration.version.value)
            }
        }
        return ChecksumVerificationResult.Verified
    }

    fun applyResult(transaction: JdbcTransaction, step: MigrationStep): MigrationStepResult {
        if (step.direction != MigrationDirection.APPLY) return MigrationStepResult.WrongDirection(MigrationDirection.APPLY)
        with(transaction) {
            step.metadata.apply.invoke(this)
            recordMigrationVersion(step.metadata.version.value)
            recordMigrationChecksum(step.metadata)
        }
        return MigrationStepResult.Applied
    }

    fun rollbackResult(transaction: JdbcTransaction, step: MigrationStep): MigrationStepResult {
        if (step.direction != MigrationDirection.ROLLBACK) return MigrationStepResult.WrongDirection(MigrationDirection.ROLLBACK)
        val rollback = step.metadata.rollback ?: return MigrationStepResult.MissingRollback
        with(transaction) {
            rollback.invoke(this)
            SchemaMigrationChecksumTable.deleteWhere { version eq step.metadata.version.value }
        }
        return MigrationStepResult.RolledBack
    }

    fun recordCurrentVersion(transaction: JdbcTransaction) {
        with(transaction) {
            recordMigrationVersion(CURRENT_SCHEMA_VERSION)
            recordMigrationChecksum(registry.last())
        }
    }
}
