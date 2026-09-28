package f95gm.server.config

import java.util.concurrent.CopyOnWriteArrayList
import f95gm.server.kernel.AwaitableLifecycleResource
import f95gm.server.kernel.MetricCounter
import f95gm.server.kernel.ResourceLock
import f95gm.server.kernel.TerminalCloseCoordinator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Atomic configuration snapshot; readers never observe a partially rebuilt configuration. */
internal class ServerConfigurationState(
    initial: ServerConfigSnapshot,
    private val listenerFailureHandler: ConfigurationListenerFailureHandler = ConfigurationListenerFailureHandler { _, failure ->
        Unit
    },
    private val eventSink: ConfigurationReloadEventSink = NoopConfigurationReloadEventSink,
    private val eventSinkFailureHandler: ConfigurationEventSinkFailureHandler = ConfigurationEventSinkFailureHandler { },
    publicationDispatcher: CoroutineDispatcher = Dispatchers.Default,
    maxPendingPublications: Int = ConfigurationPublicationDispatcher.DEFAULT_MAX_PENDING_PUBLICATIONS,
    publicationShutdownTimeout: Duration = 10.seconds
) : AwaitableLifecycleResource {
    @Volatile
    private var currentSnapshot = initial
    private val listeners = CopyOnWriteArrayList<ConfigurationReloadListener>()
    private val rejectedReloads = MetricCounter()
    private val listenerFailures = MetricCounter()
    private val eventSinkFailures = MetricCounter()
    private val reloadLock = ResourceLock()
    private val publicationDispatcher = ConfigurationPublicationDispatcher(
        publicationDispatcher, maxPendingPublications, publicationShutdownTimeout
    )
    private val lifecycle = TerminalCloseCoordinator()
    private val publisher = ConfigurationReloadPublisher(
        eventSink,
        listenerFailureHandler,
        ConfigurationPublicationFailureReporter { kind, failure ->
            when (kind) {
                ConfigurationPublicationFailureKind.LISTENER -> listenerFailures.increment()
                ConfigurationPublicationFailureKind.EVENT_SINK -> {
                    eventSinkFailures.increment()
                    eventSinkFailureHandler.onFailure(failure)
                }
            }
        }
    )

    val rejectedReloadCount: Long get() = rejectedReloads.get()
    val listenerFailureCount: Long get() = listenerFailures.get()
    val eventSinkFailureCount: Long get() = eventSinkFailures.get()
    val pendingPublicationCount: Int get() = publicationDispatcher.pendingCount
    val rejectedPublicationCount: Long get() = publicationDispatcher.rejectedCount
    override val isClosed: Boolean get() = lifecycle.isClosed
    val terminalFailure: Throwable? get() = lifecycle.failure

    fun current(): ServerConfigSnapshot = currentSnapshot

    fun subscribe(listener: ConfigurationReloadListener): ConfigurationSubscription {
        reloadLock.withLock {
            check(lifecycle.isOpen) { "Configuration state is closed" }
            check(!listeners.contains(listener)) { "Configuration listener is already subscribed" }
            listeners += listener
        }
        return ConfigurationSubscription { reloadLock.withLock { listeners -= listener } }
    }

    fun reload(source: ConfigurationSource): ServerConfigSnapshot {
        return reloadWithResult(source).current
    }

    fun reloadWithResult(source: ConfigurationSource): ConfigurationReloadResult {
        publicationDispatcher.checkCanWait()
        val publication = reloadLock.withLock {
            check(lifecycle.isOpen) { "Configuration state is closed" }
            val parsedNext = ServerConfigSnapshot.fromSystemProperties(source)
            val plan = prepareReload(parsedNext)
            if (plan.unchanged) enqueueUnchanged(plan, parsedNext.provenance.sourceName)
            else enqueueReload(plan)
        }
        return publicationDispatcher.await(publication)
    }

    /** Called under reloadLock so planning and admission share the same active snapshot. */
    private fun prepareReload(parsedNext: ServerConfigSnapshot): ConfigurationReloadPlan {
        val plan = ConfigurationReloadPlanner.plan(currentSnapshot, parsedNext)
        when (val decision = plan.decision) {
            is ConfigurationReloadDecision.RequiresRestart -> {
                rejectedReloads.increment()
                throw ConfigurationReloadRejected(decision.reasons)
            }
            ConfigurationReloadDecision.Apply -> Unit
        }
        return plan
    }

    private fun enqueueUnchanged(
        plan: ConfigurationReloadPlan,
        sourceName: String
    ): PublicationTask<ConfigurationReloadResult> = publicationDispatcher.enqueue {
        val publicationResult = publisher.publish(ConfigurationPublicationCommand.Unchanged(sourceName))
        ConfigurationReloadResult(
            previous = plan.previous,
            current = plan.previous,
            impact = ReloadImpact.DYNAMIC,
            outcome = ConfigurationReloadOutcome.Unchanged,
            changes = plan.changes,
            eventSinkFailures = publicationResult.eventSinkFailures
        )
    }

    /** Admission must succeed before the snapshot changes; observers run after that commit. */
    private fun enqueueReload(plan: ConfigurationReloadPlan): PublicationTask<ConfigurationReloadResult> {
        val result = ConfigurationReloadResult(
            previous = plan.previous,
            current = plan.next,
            impact = ReloadImpact.DYNAMIC,
            outcome = ConfigurationReloadOutcome.Applied,
            changes = plan.changes
        )
        val listenersSnapshot = listeners.toList()
        return publicationDispatcher.enqueueAfterAdmission {
            val publish: () -> ConfigurationReloadResult = {
                val publicationResult = publisher.publish(ConfigurationPublicationCommand.Reload(result, listenersSnapshot))
                result.copy(
                    listenerFailures = publicationResult.listenerFailures,
                    eventSinkFailures = publicationResult.eventSinkFailures
                )
            }
            currentSnapshot = plan.next
            publish
        }
    }

    @Suppress("TooGenericExceptionCaught") // Lifecycle closure must preserve ordinary and fatal publication failures.
    override fun close() {
        publicationDispatcher.checkCanWait()
        if (!beginClose()) return
        try {
            publicationDispatcher.close()
            lifecycle.complete()
        } catch (failure: Throwable) {
            lifecycle.fail(failure)
            throw failure
        }
    }

    @Suppress("TooGenericExceptionCaught") // Lifecycle closure must preserve ordinary and fatal publication failures.
    override suspend fun closeAndJoin() {
        publicationDispatcher.checkCanWait()
        if (!beginClose()) {
            lifecycle.await()
            return
        }
        try {
            publicationDispatcher.closeAndJoin()
            lifecycle.complete()
        } catch (failure: Throwable) {
            lifecycle.fail(failure)
            throw failure
        }
    }

    private fun beginClose(): Boolean = reloadLock.withLock {
        if (!lifecycle.beginClose()) return@withLock false
        listeners.clear()
        true
    }
}
