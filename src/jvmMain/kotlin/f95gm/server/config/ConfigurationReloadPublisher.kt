package f95gm.server.config

import f95gm.server.kernel.runObserverFailureBoundary

internal enum class ConfigurationPublicationFailureKind { LISTENER, EVENT_SINK }

internal fun interface ConfigurationPublicationFailureReporter {
    fun report(kind: ConfigurationPublicationFailureKind, failure: Exception)
}

internal sealed interface ConfigurationPublicationCommand {
    data class Reload(
        val result: ConfigurationReloadResult,
        val listeners: Iterable<ConfigurationReloadListener>
    ) : ConfigurationPublicationCommand

    data class Unchanged(val source: String) : ConfigurationPublicationCommand
}

internal data class ConfigurationPublicationResult(
    val listenerFailures: Int = 0,
    val eventSinkFailures: Int = 0
)

/** Publishes an accepted reload while isolating observer failures from state publication. */
internal class ConfigurationReloadPublisher(
    private val eventSink: ConfigurationReloadEventSink,
    private val listenerFailureHandler: ConfigurationListenerFailureHandler,
    private val failureReporter: ConfigurationPublicationFailureReporter
) {
    fun publish(command: ConfigurationPublicationCommand): ConfigurationPublicationResult {
        var failures = 0
        var listenerFailures = 0
        var eventSinkFailures = 0
        when (command) {
            is ConfigurationPublicationCommand.Unchanged -> runObserverFailureBoundary(
                onFailure = { failure ->
                    failures++
                    failureReporter.report(ConfigurationPublicationFailureKind.EVENT_SINK, failure)
                }
            ) {
                eventSink.onUnchanged(command.source)
            }
            is ConfigurationPublicationCommand.Reload -> {
                runObserverFailureBoundary(
                    onFailure = { failure ->
                        eventSinkFailures++
                        failureReporter.report(ConfigurationPublicationFailureKind.EVENT_SINK, failure)
                    }
                ) {
                    eventSink.onReload(command.result)
                }
                command.listeners.forEach { listener ->
                    runObserverFailureBoundary(
                        onFailure = { failure ->
                            listenerFailures++
                            failureReporter.report(ConfigurationPublicationFailureKind.LISTENER, failure)
                            listenerFailureHandler.onFailure(listener, failure)
                        }
                    ) {
                        listener.onReload(command.result)
                    }
                }
            }
        }
        return ConfigurationPublicationResult(listenerFailures, eventSinkFailures + failures)
    }
}
