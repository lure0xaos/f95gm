package f95gm.server.persistence.database

import f95gm.server.persistence.schema.SchemaVersionTable
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.file.Files
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DatabaseBackupTest {
    @Test
    fun backupIsAtomicAndRetainsThreeGenerations() {
        val directory = Files.createTempDirectory("f95gm-backup-test")
        val database = directory.resolve("f95gm.mv.db")
        val backup = directory.resolve("f95gm.mv.db.bak")
        try {
            val generations = (1..4).map { generation ->
                val bytes = "database-generation-$generation".encodeToByteArray()
                database.writeBytes(bytes)
                createDatabaseBackup(database, backup)
                bytes
            }

            assertContentEquals(generations[3], backup.readBytes())
            assertContentEquals(generations[2], directory.resolve("f95gm.mv.db.bak.1").readBytes())
            assertContentEquals(generations[1], directory.resolve("f95gm.mv.db.bak.2").readBytes())
            assertContentEquals(generations[0], directory.resolve("f95gm.mv.db.bak.3").readBytes())
            assertTrue(
                Files.list(directory)
                    .use { files -> !files.anyMatch { it.fileName.toString().endsWith(".backup.tmp") } })
        } finally {
            Files.walk(directory).sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }

    @Test
    fun everyRetainedGenerationCanBeRestoredAsAnApplicationDatabase() {
        val directory = Files.createTempDirectory("f95gm-restore-test")
        val sourceBase = directory.resolve("source")
        val sourceFile = directory.resolve("source.mv.db")
        val backup = directory.resolve("source.mv.db.bak")
        val source = Database.connect("jdbc:h2:file:$sourceBase;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
        try {
            migrateDatabase(source)
            runCatching {
                transaction(source) {
                    TransactionManager.current().exec("SHUTDOWN")
                }
            }
            repeat(4) { createDatabaseBackup(sourceFile, backup) }

            listOf(
                backup,
                directory.resolve("source.mv.db.bak.1"),
                directory.resolve("source.mv.db.bak.2"),
                directory.resolve("source.mv.db.bak.3")
            )
                .forEachIndexed { index, generation ->
                    val restoreBase = directory.resolve("restore-$index")
                    restoreDatabaseBackup(generation, directory.resolve("restore-$index.mv.db"))
                    val restored =
                        Database.connect("jdbc:h2:file:$restoreBase;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
                    migrateDatabase(restored)
                    transaction(restored) {
                        assertEquals(
                            CURRENT_SCHEMA_VERSION,
                            SchemaVersionTable.selectAll().single()[SchemaVersionTable.version]
                        )
                    }
                }
        } finally {
            Files.walk(directory).sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }
}
