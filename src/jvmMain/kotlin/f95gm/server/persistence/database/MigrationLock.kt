package f95gm.server.persistence.database

import f95gm.server.kernel.ResourceLock

/** Process-local serialization for schema changes and rollback operations. */
internal object MigrationLock {
    private val monitor = ResourceLock()

    fun <T> withLock(block: () -> T): T = monitor.withLock(block)
}
