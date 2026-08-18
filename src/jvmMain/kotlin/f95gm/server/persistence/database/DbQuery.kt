package f95gm.server.persistence.database

import f95gm.server.state.runtime.database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

internal fun <T> dbQuery(block: org.jetbrains.exposed.v1.jdbc.JdbcTransaction.() -> T): T =
    transaction(database, statement = block)
