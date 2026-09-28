package f95gm.server.persistence.database.migration

import f95gm.server.persistence.schema.MarksTable
import f95gm.server.persistence.schema.SavedFilterOptionsTable
import f95gm.server.persistence.schema.SavedFiltersTable
import f95gm.server.persistence.schema.SessionCookiesTable
import f95gm.server.persistence.schema.SessionsTable
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

internal const val V1_SCHEMA_CHECKSUM = "f95gm-schema-v1"

internal fun v1InitialSchemaMigration(): DatabaseMigration = DatabaseMigration(
    version = DatabaseSchemaVersion(1),
    checksum = MigrationChecksum(V1_SCHEMA_CHECKSUM),
    description = "Initial schema",
    apply = JdbcTransaction::applyV1InitialSchema,
    rollback = JdbcTransaction::rollbackV1InitialSchema
)

private fun JdbcTransaction.applyV1InitialSchema() {
    SchemaUtils.create(
        SessionsTable,
        SessionCookiesTable,
        MarksTable,
        SavedFiltersTable,
        SavedFilterOptionsTable
    )
}

private fun JdbcTransaction.rollbackV1InitialSchema() {
    exec("DROP TABLE IF EXISTS saved_filter_options")
    exec("DROP TABLE IF EXISTS saved_filters")
    exec("DROP TABLE IF EXISTS marks")
    exec("DROP TABLE IF EXISTS session_cookies")
    exec("DROP TABLE IF EXISTS sessions")
}
