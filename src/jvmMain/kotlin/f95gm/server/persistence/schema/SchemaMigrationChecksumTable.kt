package f95gm.server.persistence.schema

import org.jetbrains.exposed.v1.core.Table

internal object SchemaMigrationChecksumTable : Table("schema_migration_checksum") {
    val version = integer("version")
    val checksum = varchar("checksum", 64)

    override val primaryKey = PrimaryKey(version)
}
