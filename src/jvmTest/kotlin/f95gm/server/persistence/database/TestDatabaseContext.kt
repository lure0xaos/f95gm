package f95gm.server.persistence.database

import kotlinx.coroutines.Dispatchers

internal fun testDatabaseExecutionContext(): DatabaseExecutionContext = object : DatabaseExecutionContext {
    override val dispatcher = Dispatchers.Default
}
