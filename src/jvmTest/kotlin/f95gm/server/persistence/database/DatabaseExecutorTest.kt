package f95gm.server.persistence.database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertEquals

private object TransactionProbe : Table("transaction_probe") {
    val value = varchar("value", 32)
}

class DatabaseExecutorTest {
    @Test
    fun failedTransactionRollsBackWrites() {
        val database = Database.connect(
            url = "jdbc:h2:mem:transaction_${System.nanoTime()};DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver"
        )
        val executor = DatabaseExecutor(database = { database })
        transaction(database) { SchemaUtils.create(TransactionProbe) }

        runCatching {
            executor.transaction {
                TransactionProbe.insert { it[value] = "rolled-back" }
                error("rollback")
            }
        }

        assertEquals(0L, transaction(database) { TransactionProbe.selectAll().count() })
    }
}
