package f95gm.server.persistence.database

import f95gm.server.persistence.database.migration.*
import f95gm.server.persistence.testDatabaseUrl
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
    fun migrationCleanupPolicyRetainsLegacyFixturesUntilTheNextSchemaGeneration() {
        assertEquals(true, MigrationCleanupPolicy.retains(CURRENT_SCHEMA_VERSION))
        assertEquals(false, MigrationCleanupPolicy.retains(MigrationCleanupPolicy.REMOVE_LEGACY_FIXTURES_AFTER_VERSION))
    }

    @Test
    fun migrationRegistryDescribesTheCurrentSchemaAndIsContiguous() {
        assertEquals(CURRENT_SCHEMA_VERSION, migrationRegistry.last().version.value)
        assertEquals(CURRENT_SCHEMA_CHECKSUM, migrationRegistry.last().checksum.value)
        assertEquals(migrationRegistry.indices.map { it + 1 }, migrationRegistry.map { it.version.value })
    }

    @Test
    fun migrationRegistryRejectsSkippedVersionsAndBlankChecksums() {
        assertFailsWith<IllegalArgumentException> {
            validateMigrationRegistry(listOf(migration(1, "v1", "one"), migration(3, "v3", "three")))
        }
        assertFailsWith<IllegalArgumentException> {
            validateMigrationRegistry(listOf(migration(1, "", "one")))
        }
    }

    @Test
    fun migrationPlanMakesRollbackSupportExplicit() {
        val registry = listOf(
            migration(1, "v1", "one", rollbackSupported = true),
            migration(2, "v2", "two", rollbackSupported = false)
        )
        assertEquals(
            listOf(MigrationDirection.APPLY, MigrationDirection.APPLY),
            migrationPlan(0, 2, registry).map(MigrationStep::direction)
        )
        assertFailsWith<IllegalArgumentException> { migrationPlan(2, 0, registry) }
    }

    @Test
    fun migrationIsRepeatableAndRecordsTheCurrentVersion() {
        val database = Database.connect(
            url = testDatabaseUrl("migration"),
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
    fun registeredRollbackRunsTransactionallyAndRewindsVersion() {
        val database = Database.connect(
            url = testDatabaseUrl("rollback"),
            driver = "org.h2.Driver"
        )

        migrateDatabase(database)
        rollbackDatabase(database, 0)

        transaction(database) {
            assertEquals(0, SchemaVersionTable.selectAll().single()[SchemaVersionTable.version])
            assertEquals(0L, SchemaMigrationChecksumTable.selectAll().count())
        }
    }

    @Test
    fun rollbackFailureRestoresTheVersionAndSchemaAtomically() {
        val database = Database.connect(
            url = testDatabaseUrl("rollback_failure"),
            driver = "org.h2.Driver"
        )
        migrateDatabase(database)

        assertFailsWith<IllegalStateException> {
            rollbackDatabase(database, 0) { error("injected rollback failure") }
        }

        transaction(database) {
            assertEquals(CURRENT_SCHEMA_VERSION, SchemaVersionTable.selectAll().single()[SchemaVersionTable.version])
            assertEquals(CURRENT_SCHEMA_CHECKSUM, SchemaMigrationChecksumTable.selectAll().single()[SchemaMigrationChecksumTable.checksum])
            assertEquals(0L, SessionsTable.selectAll().count())
        }
    }

    @Test
    fun upgradesAnExplicitLegacyVersionZeroFixture() {
        val database = Database.connect(
            url = testDatabaseUrl("legacy_migration"),
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
            url = testDatabaseUrl("checksum"),
            driver = "org.h2.Driver"
        )
        migrateDatabase(database)
        transaction(database) {
            SchemaMigrationChecksumTable.update({ SchemaMigrationChecksumTable.version eq CURRENT_SCHEMA_VERSION }) {
                it[checksum] = "changed"
            }
        }
        assertFailsWith<IllegalArgumentException> { migrateDatabase(database) }
        transaction(database) {
            assertEquals(0L, SessionsTable.selectAll().count())
        }
    }

    @Test
    fun `rejects a database from the future`() {
        val database = Database.connect(
            url = testDatabaseUrl("future_migration"),
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
        val url = testDatabaseUrl("concurrent_migration")
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

    private fun migration(
        version: Int,
        checksum: String,
        description: String,
        rollbackSupported: Boolean = false
) = DatabaseMigration(
        version = DatabaseSchemaVersion(version),
        checksum = MigrationChecksum(checksum),
        description = description,
        apply = {},
        rollback = if (rollbackSupported) ({}) else null
    )
}
