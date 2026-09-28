package f95gm.server.state.runtime

import f95gm.server.kernel.ServerInvariantViolation

import f95gm.server.kernel.FailureBoundary
import f95gm.server.kernel.ResourceLock
import f95gm.server.kernel.LifecycleResource
import f95gm.server.kernel.LifecycleResourceName
import f95gm.server.kernel.AwaitableLifecycleResource
import f95gm.server.kernel.LifecycleState
import f95gm.server.kernel.ValidatedState
import f95gm.server.observability.LifecycleStateChangedEvent
import f95gm.server.observability.logger
import f95gm.server.observability.structuredLog

/** Owns lifecycle registration, dependency ordering, and the atomic transition into shutdown. */
internal class LifecycleRegistrationStore(private val resourceName: LifecycleResourceName) {
    internal sealed interface AddResult {
        data object CloseImmediately : AddResult
        data object Registered : AddResult
        data object Duplicate : AddResult
    }
    internal data class Registration(
        val name: LifecycleResourceName,
        val resource: LifecycleResource,
        val dependsOn: Set<LifecycleResourceName>,
        val awaitable: AwaitableLifecycleResource?,
        val failureBoundary: FailureBoundary
    )

    private val lock = ResourceLock()
    private val shutdown = LifecycleShutdownCoordinator()
    private val resources = RegistrationCollection()
    private val state = ValidatedState(LifecycleState.OPEN, allowedTransitions)

    val lifecycleState: LifecycleState get() = state.current()
    val isClosed: Boolean get() = lifecycleState != LifecycleState.OPEN
    val completion: LifecycleShutdownCoordinator get() = shutdown
    val terminalFailure: Throwable? get() = shutdown.failure

    fun add(
        name: LifecycleResourceName,
        resource: LifecycleResource,
        dependsOn: Set<LifecycleResourceName>,
        awaitable: AwaitableLifecycleResource?,
        failureBoundary: FailureBoundary
    ): AddResult = lock.withLock {
        if (state.current() != LifecycleState.OPEN) return@withLock AddResult.CloseImmediately
        if (resources.contains(name)) return@withLock AddResult.Duplicate
        val registration = Registration(name, resource, dependsOn, awaitable, failureBoundary)
        when (val validation = LifecycleDependencyGraph.validateResult(resources.with(registration).associate { it.name to it.dependsOn })) {
            LifecycleDependencyGraph.ValidationResult.Valid -> Unit
            is LifecycleDependencyGraph.ValidationResult.Invalid -> throw ServerInvariantViolation(validation.message)
        }
        resources.add(registration)
        AddResult.Registered
    }

    fun beginClose(): List<Registration>? = lock.withLock {
        if (state.current() != LifecycleState.OPEN || !shutdown.tryStart()) return@withLock null
        state.transition(LifecycleState.CLOSING)
        val byName = resources.snapshotAndClear().associateBy { it.name }
        val graph = byName.mapValues { (_, registration) -> registration.dependsOn }
        LifecycleDependencyGraph.dependencyFirstOrder(graph).asReversed().map(byName::getValue)
    }

    fun markCompleted() = transition(LifecycleState.COMPLETED)
    fun markFailed() = transition(LifecycleState.FAILED)

    private fun transition(next: LifecycleState) = lock.withLock {
        state.transition(next)
        logger.info { structuredLog(LifecycleStateChangedEvent(resourceName, next)) }
    }

    private companion object {
        val allowedTransitions = mapOf(
            LifecycleState.OPEN to setOf(LifecycleState.CLOSING),
            LifecycleState.CLOSING to setOf(LifecycleState.COMPLETED, LifecycleState.FAILED),
            LifecycleState.COMPLETED to emptySet(),
            LifecycleState.FAILED to emptySet()
        )
    }
}

private class RegistrationCollection {
    private val values = mutableListOf<LifecycleRegistrationStore.Registration>()

    fun add(value: LifecycleRegistrationStore.Registration) { values += value }

    fun contains(name: LifecycleResourceName): Boolean = values.any { it.name == name }

    fun with(value: LifecycleRegistrationStore.Registration): List<LifecycleRegistrationStore.Registration> = values + value

    fun snapshotAndClear(): List<LifecycleRegistrationStore.Registration> = values.toList().also { values.clear() }
}
