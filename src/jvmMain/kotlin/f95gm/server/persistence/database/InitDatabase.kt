package f95gm.server.persistence.database

import f95gm.server.config.database.databaseProperty
import f95gm.server.config.logging.logger
import f95gm.server.persistence.schema.persistenceTables
import f95gm.server.state.runtime.database
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories

@Suppress("SpreadOperator")
internal fun initDatabase() {
    val configured = System.getProperty(databaseProperty, defaultDatabasePath()).trim()
    val path = configured.takeUnless { it.startsWith("jdbc:") }
        ?.let { Path.of(it).toAbsolutePath().normalize() }
    val url = path?.let {
        it.parent?.createDirectories()
        "jdbc:h2:file:${it.absolutePathString().replace('\\', '/')};DB_CLOSE_DELAY=-1"
    } ?: configured
    logger.info {
        "Using database ${path?.absolutePathString() ?: "configured JDBC URL"}"
    }
    database = Database.connect(url = url, driver = "org.h2.Driver")
    transaction(database) {
        SchemaUtils.create(*persistenceTables)
    }
}
