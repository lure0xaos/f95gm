package f95gm.server.state.runtime

import f95gm.server.kernel.ServerClock
import f95gm.server.platform.SystemServerClock

import f95gm.server.config.ServerConfig
import f95gm.server.persistence.ServerRepositories
import f95gm.server.persistence.database.DatabaseExecutor
import io.ktor.client.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal class ServerContext(
    config: ServerConfig,
    clock: ServerClock = SystemServerClock,
    repositories: ServerRepositories? = null,
    httpClient: HttpClient? = null,
    databaseExecutor: DatabaseExecutor,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) : AutoCloseable {
    private val services = ServerRuntimeServices.create(
        config,
        clock,
        ServerRuntimeDependencies(
            database = databaseExecutor,
            repositories = repositories?.let(RepositoryDependency::Provided) ?: RepositoryDependency.Default,
            httpClient = httpClient?.let(HttpClientDependency::Provided) ?: HttpClientDependency.Default,
            dispatcher = dispatcher
        )
    )
    val applicationComposition: ServerApplicationComposition get() = services.applicationComposition
    override fun close() = services.close()
}

internal fun testServerContext(
    config: ServerConfig = ServerConfig.from(emptyMap()),
    clock: ServerClock = SystemServerClock,
    repositories: ServerRepositories? = null,
    httpClient: HttpClient? = null,
    databaseExecutor: DatabaseExecutor = DatabaseExecutor(database = { error("database not used by test") }, executionContext = f95gm.server.persistence.database.testDatabaseExecutionContext()),
    dispatcher: CoroutineDispatcher = Dispatchers.Default
): ServerContext = ServerContext(config, clock, repositories, httpClient, databaseExecutor, dispatcher)

internal inline fun <T> withTestServerContext(
    config: ServerConfig = ServerConfig.from(emptyMap()),
    clock: ServerClock = SystemServerClock,
    repositories: ServerRepositories? = null,
    httpClient: HttpClient? = null,
    databaseExecutor: DatabaseExecutor = DatabaseExecutor(database = { error("database not used by test") }, executionContext = f95gm.server.persistence.database.testDatabaseExecutionContext()),
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
