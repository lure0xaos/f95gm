package f95gm.server.persistence.database

import f95gm.server.persistence.schema.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

private val migrationJvmLock = Any()

internal const val CURRENT_SCHEMA_VERSION = 1
internal const val CURRENT_SCHEMA_CHECKSUM = "f95gm-schema-v1"

internal fun migrateDatabase(database: Database) {
    synchronized(migrationJvmLock) {
        transaction(database) {
            TransactionManager.current().exec("CREATE TABLE IF NOT EXISTS schema_migration_lock (id INT PRIMARY KEY)")
            TransactionManager.current().exec("MERGE INTO schema_migration_lock (id) KEY(id) VALUES (1)")
            TransactionManager.current().exec("SELECT id FROM schema_migration_lock WHERE id = 1 FOR UPDATE")

            SchemaUtils.create(SchemaVersionTable)
            SchemaUtils.create(SchemaMigrationChecksumTable)
            val current = SchemaVersionTable.selectAll().singleOrNull()?.get(SchemaVersionTable.version) ?: 0
            require(current <= CURRENT_SCHEMA_VERSION) { "Unsupported database schema version: $current" }
            val recordedChecksum = SchemaMigrationChecksumTable.selectAll()
                .where { SchemaMigrationChecksumTable.version eq CURRENT_SCHEMA_VERSION }
                .singleOrNull()
                ?.get(SchemaMigrationChecksumTable.checksum)
            require(recordedChecksum == null || recordedChecksum == CURRENT_SCHEMA_CHECKSUM) {
                "Database migration checksum mismatch for schema version $CURRENT_SCHEMA_VERSION"
            }
            if (current < 1) {
                SchemaUtils.create(
                    SessionsTable,
                    SessionCookiesTable,
                    MarksTable,
                    SavedFiltersTable,
                    SavedFilterOptionsTable
                )
                if (SchemaVersionTable.selectAll().none()) {
                    SchemaVersionTable.insert {
                        it[id] = 1
                        it[version] = 1
                    }
                } else {
                    SchemaVersionTable.update({ SchemaVersionTable.id eq 1 }) {
                        it[version] = 1
                    }
                }
            }
            if (SchemaMigrationChecksumTable.selectAll().none()) {
                SchemaMigrationChecksumTable.insert {
                    it[version] = CURRENT_SCHEMA_VERSION
                    it[checksum] = CURRENT_SCHEMA_CHECKSUM
                }
            }
        }
    }
}
