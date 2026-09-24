package f95gm.server.state.runtime

import f95gm.server.config.ServerConfig
import f95gm.server.http.request.RequestMetrics
import f95gm.server.persistence.database.DatabaseExecutor
import f95gm.server.state.runtime.session.SessionRegistry
import f95gm.server.upstream.request.UpstreamHttpClient
import f95gm.server.upstream.request.UpstreamPolicy
import kotlinx.coroutines.CoroutineScope

internal data class ServerUpstreamDependencies(
    val client: UpstreamHttpClient,
    val scope: CoroutineScope,
    val config: ServerConfig,
    val policy: UpstreamPolicy,
    val clock: ServerClock,
    val sessions: SessionRegistry,
    val database: DatabaseExecutor,
    val metrics: RequestMetrics
)
