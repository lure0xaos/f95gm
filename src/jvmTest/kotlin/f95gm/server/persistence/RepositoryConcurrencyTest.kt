package f95gm.server.persistence

import f95gm.server.persistence.database.testDatabaseExecutionContext

import f95gm.domain.tracking.GameTrackingState
import f95gm.domain.values.F95AccountKey
import f95gm.domain.values.F95OptionId
import f95gm.domain.values.F95OptionName
import f95gm.domain.values.F95ThreadId
import f95gm.domain.values.StoredVersion
import f95gm.server.model.EpochMillis
import f95gm.server.model.ConditionalWriteResult
import f95gm.server.model.marks.GameMark
import f95gm.server.model.filters.SavedFilter
import f95gm.domain.api.FilterOptionRequest
import f95gm.server.persistence.schema.SavedFilterOptionsTable
import f95gm.server.persistence.database.DatabaseExecutor
import f95gm.server.persistence.database.migrateDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RepositoryConcurrencyTest {
    @Test
    fun savedFilterOptionKindsKeepTheirStorageKeysAcrossPersistenceRoundTrip() = runTest {
        val database = Database.connect(
            url = testDatabaseUrl("repository_filter_kinds"),
            driver = "org.h2.Driver"
        )
        migrateDatabase(database)
        val repositories = ServerRepositories(
            DatabaseExecutor(database = { database }, executionContext = testDatabaseExecutionContext())
        )
        val account = F95AccountKey("account")
        val filter = SavedFilter(
            name = F95OptionName("All option kinds"),
            selectedTags = listOf(FilterOptionRequest(F95OptionId("tag-in"), F95OptionName("Tag in"))),
            excludedTags = listOf(FilterOptionRequest(F95OptionId("tag-out"), F95OptionName("Tag out"))),
            selectedPrefixes = listOf(FilterOptionRequest(F95OptionId("prefix-in"), F95OptionName("Prefix in"))),
            excludedPrefixes = listOf(FilterOptionRequest(F95OptionId("prefix-out"), F95OptionName("Prefix out")))
        )

        val stored = repositories.savedFilters.store(account, filter)

        assertEquals(listOf(stored), repositories.savedFilters.load(account))
        val storedKinds = DatabaseExecutor(
            database = { database }, executionContext = testDatabaseExecutionContext()
        ).transactionSuspending {
            SavedFilterOptionsTable.selectAll().map { it[SavedFilterOptionsTable.kind] }.toSet()
        }
        assertEquals(
            setOf("selectedTags", "excludedTags", "selectedPrefixes", "excludedPrefixes"),
            storedKinds
        )
    }

    @Test
    fun independentAccountsCanWriteConcurrentlyWithoutCrossAccountLoss() = runTest {
        val database = Database.connect(
            url = testDatabaseUrl("repository_concurrent"),
            driver = "org.h2.Driver"
        )
        migrateDatabase(database)
        val repositories = ServerRepositories(DatabaseExecutor(database = { database }, executionContext = testDatabaseExecutionContext()))
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
            url = testDatabaseUrl("repository"),
            driver = "org.h2.Driver"
        )
        migrateDatabase(database)
        val repositories = ServerRepositories(DatabaseExecutor(database = { database }, executionContext = testDatabaseExecutionContext()))
        val account = F95AccountKey("account")
        val mark = GameMark(
            threadId = F95ThreadId("123"),
            trackingState = GameTrackingState.WATCHING,
            storedVersion = StoredVersion("1.0"),
            updatedAt = EpochMillis(20)
        )

        assertEquals(ConditionalWriteResult.Stored(Unit), repositories.marks.storeIfCurrent(account, mark, null))
        assertEquals(
            ConditionalWriteResult.Conflict(expectedUpdatedAt = EpochMillis(10L), actualUpdatedAt = EpochMillis(20L)),
            repositories.marks.storeIfCurrent(account, mark.copy(updatedAt = EpochMillis(30)), EpochMillis(10L))
        )
        assertEquals(listOf(mark), repositories.marks.load(account))
    }

    @Test
    fun failedSavedFilterWriteRollsBackFilterAndOptionsTogether() = runTest {
        val database = Database.connect(
            url = testDatabaseUrl("repository_filter_rollback"),
            driver = "org.h2.Driver"
        )
        migrateDatabase(database)
        val repositories = ServerRepositories(DatabaseExecutor(database = { database }, executionContext = testDatabaseExecutionContext()))
        val duplicateOption = FilterOptionRequest(F95OptionId("tag-1"), F95OptionName("Tag"))

        assertFailsWith<Exception> {
            repositories.savedFilters.store(
                F95AccountKey("account"),
                SavedFilter(
                    name = f95gm.domain.values.F95OptionName("Rollback"),
                    selectedTags = listOf(duplicateOption, duplicateOption)
                )
            )
        }

        assertEquals(emptyList(), repositories.savedFilters.load(F95AccountKey("account")))
    }
}

