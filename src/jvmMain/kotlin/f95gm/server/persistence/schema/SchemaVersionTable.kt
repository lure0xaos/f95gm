package f95gm.server.persistence.schema

import org.jetbrains.exposed.v1.core.Table

internal object SchemaVersionTable : Table("schema_version") {
    val id = integer("id")
    val version = integer("version")

    override val primaryKey = PrimaryKey(id)
}
