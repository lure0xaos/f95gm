package f95gm.server.persistence

import f95gm.domain.tracking.GameTrackingState
import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95ThreadId
import f95gm.domain.values.StoredVersion
import f95gm.server.model.EpochMillis
import f95gm.server.model.marks.GameMark
import f95gm.server.persistence.database.DatabaseExecutor
import f95gm.server.persistence.database.migrateDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.jdbc.Database
import kotlin.test.Test
import kotlin.test.assertEquals

class RepositoryConcurrencyTest {
    @Test
    fun independentAccountsCanWriteConcurrentlyWithoutCrossAccountLoss() = runTest {
        val database = Database.connect(
            url = "jdbc:h2:mem:repository_concurrent_${System.nanoTime()};DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver"
        )
        migrateDatabase(database)
        val repositories = ServerRepositories(DatabaseExecutor(database = { database }))
        val mark = GameMark(
            threadId = F95ThreadId("123"),
            trackingState = GameTrackingState.WATCHING,
            storedVersion = StoredVersion("1.0"),
            updatedAt = EpochMillis(20)
        )

        coroutineScope {
            (1..16).map { index ->
                async(Dispatchers.IO) {
                    repositories.marks.store(F95AccountKey("account-$index"), mark)
                }
            }.awaitAll()
        }

        (1..16).forEach { index ->
            assertEquals(listOf(mark), repositories.marks.load(F95AccountKey("account-$index")))
        }
    }

    @Test
    fun staleMarkWriteIsRejectedWithoutOverwritingTheCurrentValue() = runTest {
        val database = Database.connect(
            url = "jdbc:h2:mem:repository_${System.nanoTime()};DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver"
        )
        migrateDatabase(database)
        val repositories = ServerRepositories(DatabaseExecutor(database = { database }))
        val account = F95AccountKey("account")
        val mark = GameMark(
            threadId = F95ThreadId("123"),
            trackingState = GameTrackingState.WATCHING,
            storedVersion = StoredVersion("1.0"),
            updatedAt = EpochMillis(20)
        )

        assertEquals(ConditionalWriteResult.Stored(Unit), repositories.marks.storeIfCurrent(account, mark, null))
        assertEquals(
            ConditionalWriteResult.Conflict,
            repositories.marks.storeIfCurrent(account, mark.copy(updatedAt = EpochMillis(30)), 10L)
        )
        assertEquals(listOf(mark), repositories.marks.load(account))
    }
}
