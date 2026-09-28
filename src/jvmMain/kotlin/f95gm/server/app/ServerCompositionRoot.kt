package f95gm.server.app

import f95gm.server.config.ServerConfig
import f95gm.server.http.request.ratelimit.RateLimitPolicy
import f95gm.server.persistence.database.BlockingDelayScheduler
import f95gm.server.persistence.database.initDatabase
import f95gm.server.kernel.ServerClock
import f95gm.server.platform.SystemServerClock
import f95gm.server.platform.SystemConfigurationSource
import f95gm.server.state.runtime.ServerRuntimeDependencies
import f95gm.server.state.runtime.ServerRuntimeServices
import f95gm.server.state.runtime.DatabaseComposition
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.jdbc.Database
import java.nio.file.Path

private fun defaultDatabasePath(): String =
    Path.of(SystemConfigurationSource.property("user.home").orEmpty(), "F95GM", "f95gm").toString()

internal data class ServerCompositionDependencies(
    val databaseFactory: (ServerConfig) -> Database = { config -> initDatabase(config, defaultDatabasePath()) },
    val clock: ServerClock = SystemServerClock,
    val dispatchers: ServerDispatchers = ServerDispatchers(),
    val databaseDelayScheduler: BlockingDelayScheduler =
        f95gm.server.persistence.database.InterruptibleBlockingDelayScheduler()
)

/** Centralizes construction of the application graph while keeping startup orchestration small. */
internal object ServerCompositionRoot {
    fun handle(config: ServerConfig): ServerCompositionHandle = handle(config, ServerCompositionDependencies())

    fun handle(config: ServerConfig, database: Database): ServerCompositionHandle = handle(
        config,
        ServerCompositionDependencies(databaseFactory = { database })
    )

    fun handle(config: ServerConfig, dependencies: ServerCompositionDependencies): ServerCompositionHandle {
        val database = dependencies.databaseFactory(config)
        val services = ServerRuntimeServices.create(
            config = config,
            clock = dependencies.clock,
            dependencies = ServerRuntimeDependencies(
                database = DatabaseComposition.create(
                    config,
                    database,
                    dependencies.dispatchers.database,
                    dependencies.databaseDelayScheduler
                ),
                dispatcher = dependencies.dispatchers.application
            )
        )
        return ServerCompositionHandle(services.applicationComposition, services::close)
    }

    fun rateLimitPolicy(config: ServerConfig): RateLimitPolicy = RateLimitPolicy.configured(
        config.traffic.ipRequestsPerMinute.value,
        config.traffic.accountRequestsPerMinute.value,
        config.traffic.rateLimitWindowMillis.value
    )
}
