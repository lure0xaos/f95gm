package f95gm.server.persistence.database

import f95gm.domain.values.F95EpochMillis

/** Explicit SQL-boundary representation for persisted epoch milliseconds. */
@JvmInline
internal value class DatabaseEpochMillis(val value: Long) {
    fun toDomain(): F95EpochMillis = F95EpochMillis(value)

    companion object {
        fun fromDomain(value: F95EpochMillis): DatabaseEpochMillis = DatabaseEpochMillis(value.value)
    }
}
