package f95gm.server.persistence.database

import f95gm.server.observability.DatabaseOperation

/** Persistence-facing transaction boundary; callers do not depend on database construction details. */
internal interface TransactionBoundary {
    fun <T> transaction(block: org.jetbrains.exposed.v1.jdbc.JdbcTransaction.() -> T): T
    fun <T> transaction(
        operation: DatabaseOperation,
        block: org.jetbrains.exposed.v1.jdbc.JdbcTransaction.() -> T
    ): T
    suspend fun <T> transactionSuspending(
        operation: DatabaseOperation,
        block: org.jetbrains.exposed.v1.jdbc.JdbcTransaction.() -> T
    ): T
    suspend fun <T> transactionSuspending(
        block: org.jetbrains.exposed.v1.jdbc.JdbcTransaction.() -> T
    ): T = transactionSuspending(DatabaseOperation.TRANSACTION, block)
}
