package f95gm.server.state.runtime

import f95gm.server.config.ServerConfig
import f95gm.server.persistence.database.BlockingDelayScheduler
import f95gm.server.persistence.database.DatabaseExecutionContext
import f95gm.server.persistence.database.DatabaseExecutor
import f95gm.server.persistence.database.DatabaseRetryPolicy
import kotlinx.coroutines.CoroutineDispatcher
import org.jetbrains.exposed.v1.jdbc.Database
import kotlin.time.Duration.Companion.milliseconds
import f95gm.server.observability.DatabaseOperationTelemetry

/** Owns construction of persistence infrastructure and its execution policy. */
internal object DatabaseComposition {
    fun create(
        config: ServerConfig,
        database: Database,
        databaseDispatcher: CoroutineDispatcher,
        delayScheduler: BlockingDelayScheduler
    ): DatabaseExecutor = DatabaseExecutor(
        database = { database },
        retryPolicy = DatabaseRetryPolicy(
            maxTotalRetryDelay = config.databaseRetry.maxTotalDelayMillis.value.milliseconds
        ),
        delayScheduler = delayScheduler,
        executionContext = object : DatabaseExecutionContext {
            override val dispatcher: CoroutineDispatcher = databaseDispatcher
        },
        telemetry = DatabaseOperationTelemetry.logging
    )
}
