package f95gm.server.persistence.database

import f95gm.server.config.ServerConfig
import f95gm.server.observability.*
import f95gm.server.kernel.BoundaryFailure
import f95gm.server.kernel.FailureCategory
import f95gm.server.kernel.FailureBoundary
import f95gm.server.observability.FailureScope
import f95gm.server.model.DatabasePath
import org.jetbrains.exposed.v1.jdbc.Database
import java.nio.file.Path
import kotlin.io.path.*

internal class DatabaseInitializationFailure(cause: Exception) :
    DatabaseInvariantViolation("Database initialization failed", cause)

internal open class DatabaseInvariantViolation(message: String, cause: Throwable? = null) :
    BoundaryFailure(message, cause, FailureCategory.DATABASE)

internal class DatabaseMigrationFailure(cause: Throwable) :
    DatabaseInvariantViolation("Database migration or rollback failed", cause)

internal class DatabaseBackupInvariantViolation(message: String) : DatabaseInvariantViolation(message)

@Suppress("SpreadOperator")
internal fun initDatabase(config: ServerConfig, fallbackDatabasePath: String): Database {
    val configured = DatabasePath(config.databaseLocation.value.ifBlank { fallbackDatabasePath })
    val path = configured.value.takeUnless { it.startsWith("jdbc:") }
        ?.let { Path.of(it).toAbsolutePath().normalize() }
    val url = path?.let {
        it.parent?.createDirectories()
        "jdbc:h2:file:${it.absolutePathString().replace('\\', '/')};DB_CLOSE_DELAY=-1"
    } ?: configured.value
    logger.info {
        "Using database ${path?.absolutePathString() ?: "configured JDBC URL"}"
    }
    val databaseFile = path?.resolveSibling("${path.fileName}.mv.db")
    val backupFile = databaseFile?.resolveSibling("${databaseFile.fileName}.bak")
    if (databaseFile?.exists() == true && backupFile != null) {
        FailureReporter.runBoundary(FailureScope("database-backup"), FailureBoundary.PERSISTENCE, ::DatabaseInitializationFailure) {
            createDatabaseBackup(databaseFile, backupFile)
            logger.info {
                structuredLog(DatabaseLifecycleEvent(
                    DatabaseLifecycleEventName.BACKUP_CREATED,
                    listOf(logField(StructuredLogKeys.PATH, backupFile.absolutePathString()))
                ))
            }
        }
    }
    val database = Database.connect(url = url, driver = "org.h2.Driver")
    try {
        FailureReporter.runBoundary(FailureScope("database-migration"), FailureBoundary.PERSISTENCE, ::DatabaseInitializationFailure) {
            migrateDatabase(database)
        }
    } catch (error: DatabaseInitializationFailure) {
        logger.error(error) {
            structuredLog(DatabaseLifecycleEvent(
                DatabaseLifecycleEventName.MIGRATION_FAILED,
                listOf(logField(StructuredLogKeys.BACKUP, backupFile?.absolutePathString() ?: "configured JDBC backup"))
            ))
        }
        throw error
    }
    return database
}
