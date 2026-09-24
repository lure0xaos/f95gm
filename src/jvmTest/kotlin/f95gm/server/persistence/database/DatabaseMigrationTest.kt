package f95gm.server.persistence.database

import f95gm.server.persistence.schema.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DatabaseMigrationTest {
    @Test
    fun migrationIsRepeatableAndRecordsTheCurrentVersion() {
        val database = Database.connect(
            url = "jdbc:h2:mem:migration_${System.nanoTime()};DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver"
        )

        migrateDatabase(database)
        migrateDatabase(database)

        transaction(database) {
            assertEquals(1, SchemaVersionTable.selectAll().count())
            assertEquals(CURRENT_SCHEMA_VERSION, SchemaVersionTable.selectAll().single()[SchemaVersionTable.version])
            assertEquals(
                CURRENT_SCHEMA_CHECKSUM,
                SchemaMigrationChecksumTable.selectAll().single()[SchemaMigrationChecksumTable.checksum]
            )
            assertEquals(0L, MarksTable.selectAll().count())
            assertEquals(0L, SavedFiltersTable.selectAll().count())
        }
    }

    @Test
    fun upgradesAnExplicitLegacyVersionZeroFixture() {
        val database = Database.connect(
            url = "jdbc:h2:mem:legacy_migration_${System.nanoTime()};DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver"
        )
        transaction(database) {
            org.jetbrains.exposed.v1.jdbc.SchemaUtils.create(SchemaVersionTable, SchemaMigrationChecksumTable)
            SchemaVersionTable.insert {
                it[id] = 1
                it[version] = 0
            }
        }

        migrateDatabase(database)

        transaction(database) {
            assertEquals(CURRENT_SCHEMA_VERSION, SchemaVersionTable.selectAll().single()[SchemaVersionTable.version])
            assertEquals(
                CURRENT_SCHEMA_CHECKSUM,
                SchemaMigrationChecksumTable.selectAll().single()[SchemaMigrationChecksumTable.checksum]
            )
            assertEquals(0L, SessionsTable.selectAll().count())
        }
    }

    @Test
    fun rejectsAChangedHistoricalMigrationChecksum() {
        val database = Database.connect(
            url = "jdbc:h2:mem:checksum_${System.nanoTime()};DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver"
        )
        migrateDatabase(database)
        transaction(database) {
            SchemaMigrationChecksumTable.update({ SchemaMigrationChecksumTable.version eq CURRENT_SCHEMA_VERSION }) {
                it[checksum] = "changed"
            }
        }
        assertFailsWith<IllegalArgumentException> { migrateDatabase(database) }
    }

    @Test
    fun `rejects a database from the future`() {
        val database = Database.connect(
            url = "jdbc:h2:mem:future_migration_${System.nanoTime()};DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver"
        )
        transaction(database) {
            org.jetbrains.exposed.v1.jdbc.SchemaUtils.create(SchemaVersionTable)
            SchemaVersionTable.insert {
                it[id] = 1
                it[version] = CURRENT_SCHEMA_VERSION + 1
            }
        }

        assertFailsWith<IllegalArgumentException> { migrateDatabase(database) }
    }

    @Test
    fun concurrentInitializationsShareOneMigrationLock() {
        val url = "jdbc:h2:mem:concurrent_migration_${System.nanoTime()};DB_CLOSE_DELAY=-1"
        val databases = List(2) {
            Database.connect(url = url, driver = "org.h2.Driver")
        }
        val executor = Executors.newFixedThreadPool(2)
        try {
            val futures = databases.map { database -> executor.submit { migrateDatabase(database) } }
            futures.forEach { it.get() }
        } finally {
            executor.shutdownNow()
        }

        transaction(databases.first()) {
            assertEquals(1L, TransactionManager.current().exec("SELECT COUNT(*) FROM schema_migration_lock") { result ->
                result.next()
                result.getLong(1)
            })
        }
    }
}
