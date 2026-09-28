package f95gm.server.persistence.database

import f95gm.server.persistence.schema.MarksTable
import f95gm.server.persistence.schema.SchemaMigrationChecksumTable
import f95gm.server.persistence.schema.SchemaVersionTable
import f95gm.server.persistence.schema.SavedFilterOptionsTable
import f95gm.server.persistence.schema.SavedFiltersTable
import f95gm.server.persistence.schema.SessionCookiesTable
import f95gm.server.persistence.schema.SessionsTable
import f95gm.server.persistence.database.migration.DatabaseMigration
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

internal fun JdbcTransaction.ensureSchemaTables() {
    SchemaUtils.create(
        SessionsTable,
        SessionCookiesTable,
        MarksTable,
        SavedFiltersTable,
        SavedFilterOptionsTable,
        SchemaVersionTable,
        SchemaMigrationChecksumTable
    )
}

internal fun JdbcTransaction.restoreCurrentSchemaMetadata() {
    recordMigrationVersion(CURRENT_SCHEMA_VERSION)
    recordMigrationChecksum(migrationRegistry.last())
}

internal fun recordMigrationVersion(version: Int) {
    if (SchemaVersionTable.selectAll().none()) {
        SchemaVersionTable.insert {
            it[id] = 1
            it[SchemaVersionTable.version] = version
        }
    } else {
        SchemaVersionTable.update({ SchemaVersionTable.id eq 1 }) {
            it[SchemaVersionTable.version] = version
        }
    }
}

internal fun recordMigrationChecksum(migration: DatabaseMigration) {
    val existing = SchemaMigrationChecksumTable.selectAll()
        .where { SchemaMigrationChecksumTable.version eq migration.version.value }
        .singleOrNull()
    if (existing == null) {
        SchemaMigrationChecksumTable.insert {
            it[version] = migration.version.value
            it[checksum] = migration.checksum.value
        }
    } else {
        SchemaMigrationChecksumTable.update({ SchemaMigrationChecksumTable.version eq migration.version.value }) {
            it[checksum] = migration.checksum.value
        }
    }
}
