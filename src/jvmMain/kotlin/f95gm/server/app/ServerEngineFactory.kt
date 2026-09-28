package f95gm.server.app

import f95gm.domain.api.F95ApiPaths
import f95gm.server.config.redactedSummary
import f95gm.server.desktop.ServerShutdownController
import f95gm.server.http.logging.redactLogValue
import f95gm.server.http.request.RequestIdentifierFactory
import f95gm.server.http.request.ShutdownToken
import f95gm.server.state.runtime.ServerApplicationComposition
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.request.*
import org.slf4j.event.Level
import io.ktor.server.cio.CIO as ServerCIO

internal fun createServerEngine(
    config: f95gm.server.config.ServerConfig,
    shutdownToken: ShutdownToken,
    shutdownController: ServerShutdownController,
    composition: ServerApplicationComposition,
    identifiers: RequestIdentifierFactory,
    healthState: ServerHealthState,
) = embeddedServer(ServerCIO, host = config.serverHost.value, port = config.port.value) {
    configureStatusPages()
    install(CallLogging) {
        disableDefaultColors()
        level = Level.INFO
        filter { call -> call.request.uri.substringBefore('?') != F95ApiPaths.HEALTH }
        format { call ->
            val status = call.response.status()?.value ?: "-"
            "${call.request.httpMethod.value} ${redactLogValue(call.request.uri.substringBefore('?'))} -> $status"
        }
    }
    configureRoutes(
        shutdownToken,
        { shutdownController },
        RouteConfiguration(
            maxRequestBodyBytes = config.limits.maxRequestBodyBytes,
            rateLimitPolicy = ServerCompositionRoot.rateLimitPolicy(config),
            maxRateLimitBuckets = RateLimitBucketLimit(config.traffic.maxRateLimitBuckets.value),
            requestIdGenerator = identifiers.requestIds
        ),
        composition,
        healthState
    )
}
