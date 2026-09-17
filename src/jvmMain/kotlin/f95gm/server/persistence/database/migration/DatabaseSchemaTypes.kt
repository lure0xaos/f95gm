package f95gm.server.persistence.database.migration

@JvmInline
internal value class DatabaseSchemaVersion(val value: Int) {
    init { require(value >= 0) }
}

@JvmInline
internal value class MigrationChecksum(val value: String) {
    init { require(value.isNotBlank()) }
}
