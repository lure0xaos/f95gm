package f95gm.server.persistence.database

import f95gm.server.config.ServerConfig
import f95gm.server.config.logging.logger
import org.jetbrains.exposed.v1.jdbc.Database
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import kotlin.io.path.*

internal class DatabaseInitializationFailure(cause: Exception) :
    IllegalStateException("Database initialization failed", cause)

@Suppress("SpreadOperator")
internal fun initDatabase(config: ServerConfig): Database {
    val configured = config.database.ifBlank { defaultDatabasePath() }
    val path = configured.takeUnless { it.startsWith("jdbc:") }
        ?.let { Path.of(it).toAbsolutePath().normalize() }
    val url = path?.let {
        it.parent?.createDirectories()
        "jdbc:h2:file:${it.absolutePathString().replace('\\', '/')};DB_CLOSE_DELAY=-1"
    } ?: configured
    logger.info {
        "Using database ${path?.absolutePathString() ?: "configured JDBC URL"}"
    }
    val databaseFile = path?.resolveSibling("${path.fileName}.mv.db")
    val backupFile = databaseFile?.resolveSibling("${databaseFile.fileName}.bak")
    if (databaseFile?.exists() == true && backupFile != null) {
        try {
            createDatabaseBackup(databaseFile, backupFile)
            logger.info { "Created database migration backup ${backupFile.absolutePathString()} (retained generations: 3)" }
        } catch (error: Exception) {
            throw DatabaseInitializationFailure(error)
        }
    }
    val database = Database.connect(url = url, driver = "org.h2.Driver")
    try {
        migrateDatabase(database)
    } catch (error: Exception) {
        logger.error(error) { "Database migration failed; backup is available at ${backupFile?.absolutePathString() ?: "configured JDBC backup"}" }
        throw DatabaseInitializationFailure(error)
    }
    return database
}

private const val DATABASE_BACKUP_GENERATIONS = 3

internal fun createDatabaseBackup(databaseFile: Path, backupFile: Path) {
    val parent = databaseFile.parent ?: error("Database file must have a parent directory")
    val temporary = createTempFile(parent, "${databaseFile.fileName}.", ".backup.tmp")
    try {
        databaseFile.copyTo(temporary, overwrite = true)
        check(temporary.fileSize() == databaseFile.fileSize()) {
            "Database backup size does not match the source database"
        }
        for (generation in DATABASE_BACKUP_GENERATIONS - 1 downTo 1) {
            val current = generationPath(backupFile, generation)
            val next = generationPath(backupFile, generation + 1)
            if (current.exists()) moveReplacing(current, next)
        }
        if (backupFile.exists()) moveReplacing(backupFile, generationPath(backupFile, 1))
        moveReplacing(temporary, backupFile)
        check(backupFile.fileSize() == databaseFile.fileSize()) {
            "Database backup validation failed after atomic replacement"
        }
    } finally {
        temporary.deleteIfExists()
    }
}

private fun generationPath(backupFile: Path, generation: Int): Path =
    backupFile.resolveSibling("${backupFile.fileName}.$generation")

private fun moveReplacing(source: Path, target: Path) {
    try {
        source.moveTo(target, ATOMIC_MOVE, REPLACE_EXISTING)
    } catch (_: AtomicMoveNotSupportedException) {
        source.moveTo(target, REPLACE_EXISTING)
    }
}
