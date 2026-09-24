package f95gm.server.state.runtime

import f95gm.server.config.ServerConfig
import f95gm.server.persistence.ServerRepositories
import f95gm.server.persistence.database.DatabaseExecutor
import io.ktor.client.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal fun testServerContext(
    config: ServerConfig = ServerConfig.from(emptyMap()),
    clock: ServerClock = SystemServerClock,
    repositories: ServerRepositories? = null,
    httpClient: HttpClient? = null,
    databaseExecutor: DatabaseExecutor = DatabaseExecutor(database = { error("database not used by test") }),
    dispatcher: CoroutineDispatcher = Dispatchers.Default
): ServerContext = ServerContext(config, clock, repositories, httpClient, databaseExecutor, dispatcher)

internal inline fun <T> withTestServerContext(
    config: ServerConfig = ServerConfig.from(emptyMap()),
    clock: ServerClock = SystemServerClock,
    repositories: ServerRepositories? = null,
    httpClient: HttpClient? = null,
    databaseExecutor: DatabaseExecutor = DatabaseExecutor(database = { error("database not used by test") }),
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    block: (ServerContext) -> T
): T {
    val context = testServerContext(config, clock, repositories, httpClient, databaseExecutor, dispatcher)
    return try {
        block(context)
    } finally {
        context.close()
    }
}
