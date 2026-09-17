package f95gm.server.persistence.database

import f95gm.server.config.ServerConfig
import f95gm.server.persistence.schema.SchemaVersionTable
import java.nio.file.Files
import kotlin.io.path.readBytes
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class DatabaseInitializationTest {
    @Test
    fun startupBackupPreservesTheDatabaseStateFromBeforeMigration() {
        val directory = Files.createTempDirectory("f95gm-init-backup-test")
        val databaseBase = directory.resolve("app-database")
        val databaseFile = directory.resolve("app-database.mv.db")
        val backupFile = directory.resolve("app-database.mv.db.bak")
        val seedUrl = "jdbc:h2:file:${databaseBase.toAbsolutePath().toString().replace('\\', '/')}"
        val seed = Database.connect(seedUrl, driver = "org.h2.Driver")
        try {
            transaction(seed) {
                exec("DROP TABLE IF EXISTS pre_migration_marker")
                exec("CREATE TABLE pre_migration_marker (id INT PRIMARY KEY)")
            }
            val preMigrationBytes = databaseFile.readBytes()

            val initialized = initDatabase(
                ServerConfig.from(mapOf("f95gm.database" to databaseBase.toString())),
                databaseBase.toString()
            )

            transaction(initialized) {
                assertEquals(CURRENT_SCHEMA_VERSION, SchemaVersionTable.selectAll().single()[SchemaVersionTable.version])
            }
            assertContentEquals(preMigrationBytes, backupFile.readBytes())
        } finally {
            Files.walk(directory).sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }

    @Test
    fun backupFailureStopsStartupBeforeDatabaseConnectionOrMigration() {
        val directory = Files.createTempDirectory("f95gm-init-backup-failure-test")
        val databaseBase = directory.resolve("app-database")
        val databaseFile = directory.resolve("app-database.mv.db")
        try {
            Files.createDirectory(databaseFile)
            val failure = assertFailsWith<DatabaseInitializationFailure> {
                initDatabase(
                    ServerConfig.from(mapOf("f95gm.database" to databaseBase.toString())),
                    databaseBase.toString()
                )
            }

            assertEquals("Database initialization failed", failure.message)
            assertEquals(false, Files.exists(directory.resolve("app-database.mv.db.bak")))
        } finally {
            Files.walk(directory).sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }
}
