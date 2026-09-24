package f95gm.server.persistence.schema

internal val persistenceTables = arrayOf(
    SchemaVersionTable,
    SessionsTable,
    SessionCookiesTable,
    MarksTable,
    SavedFiltersTable,
    SavedFilterOptionsTable
)
