package f95gm.server.state.runtime

import f95gm.server.kernel.ServerInvariantViolation
import f95gm.server.kernel.TerminalFailureCoordinator

import f95gm.server.kernel.FailureBoundary
import f95gm.server.kernel.LifecycleResourceName
import f95gm.server.kernel.LifecycleResource
import f95gm.server.kernel.LifecycleResources
import f95gm.server.kernel.AwaitableLifecycleResource
import f95gm.server.kernel.LifecycleState
import f95gm.server.kernel.shutdownTimeoutViolation
import f95gm.server.kernel.ShutdownDeadline
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import kotlin.time.Duration

/** Owns resources and closes dependents before their dependencies with aggregated failures. */
internal class ServerLifecycle(private val policy: LifecyclePolicy = LifecyclePolicy()) : AwaitableLifecycleResource {
    internal constructor(shutdownTimeout: Duration) : this(LifecyclePolicy(shutdownTimeout))
    private val registrations = LifecycleRegistrationStore(LifecycleResources.SERVER)
    fun <T : AwaitableLifecycleResource> register(
        name: LifecycleResourceName,
        resource: T,
        dependsOn: Set<LifecycleResourceName> = emptySet(),
        failureBoundary: FailureBoundary = FailureBoundary.LIFECYCLE
    ): T = registerInternal(name, resource, resource, dependsOn, failureBoundary)

    @Suppress("TooGenericExceptionCaught") // Late registration closes through the same reporting boundary as normal lifecycle shutdown.
    fun <T : LifecycleResource> register(
        name: LifecycleResourceName,
        resource: T,
        dependsOn: Set<LifecycleResourceName> = emptySet(),
        failureBoundary: FailureBoundary = FailureBoundary.LIFECYCLE
    ): T = registerInternal(name, resource, null, dependsOn, failureBoundary)

    @Suppress("TooGenericExceptionCaught")
    private fun <T : LifecycleResource> registerInternal(
        name: LifecycleResourceName,
        resource: T,
        awaitable: AwaitableLifecycleResource?,
        dependsOn: Set<LifecycleResourceName>,
        failureBoundary: FailureBoundary
    ): T {
        val registrationResult = registrations.add(
            name,
            resource,
            dependsOn,
            awaitable,
            failureBoundary
        )
        when (registrationResult) {
            LifecycleRegistrationStore.AddResult.Registered -> Unit
            LifecycleRegistrationStore.AddResult.Duplicate ->
                throw ServerInvariantViolation("Duplicate lifecycle resource: ${name.value}")
            LifecycleRegistrationStore.AddResult.CloseImmediately -> {
            try {
                resource.close()
            } catch (failure: Exception) {
                LifecycleFailurePolicy.report(
                    LifecycleResourceName("resource-close:${name.value}"), failureBoundary, failure,
                    "Failed to close late lifecycle resource ${name.value}"
                )
                throw failure
            }
            }
        }
        return resource
    }

    override val isClosed: Boolean get() = registrations.isClosed
    val terminalFailure: Throwable? get() = registrations.terminalFailure

    override fun close() {
        val registrations = registrations.beginClose() ?: return
        val failures = CleanupFailureCollector()
        this.registrations.completion.runTerminal {
            closeSynchronously(registrations, failures)
            finishClose(failures)
        }
    }

    override suspend fun closeAndJoin() {
        val registrations = registrations.beginClose()
        if (registrations == null) {
            this.registrations.completion.await()
            return
        }
        val failures = CleanupFailureCollector()
        this.registrations.completion.runSuspendTerminal {
            try {
                withContext(ShutdownDeadline(policy.shutdownTimeout)) {
                    withTimeout(policy.shutdownTimeout) {
                        closeAwaitably(registrations, failures)
                    }
                }
                finishClose(failures)
            } catch (error: TimeoutCancellationException) {
                throw shutdownTimeoutViolation("server resources", error)
            }
        }
    }

    private fun closeSynchronously(
        registrations: List<LifecycleRegistrationStore.Registration>,
        failures: CleanupFailureCollector
    ) = LifecycleCloseExecutor.closeSynchronously(registrations, failures, ::recordFailure)

    private suspend fun closeAwaitably(
        registrations: List<LifecycleRegistrationStore.Registration>,
        failures: CleanupFailureCollector
    ) = LifecycleCloseExecutor.closeAwaitably(registrations, failures, ::recordFailure)

    private fun finishClose(failures: CleanupFailureCollector) {
        if (failures.isNotEmpty) {
            registrations.markFailed()
            throw ServerLifecycleCloseException(failures.summary())
        }
        registrations.markCompleted()
        registrations.completion.complete()
    }

    private fun recordFailure(
        registration: LifecycleRegistrationStore.Registration,
        failure: Exception,
        failures: CleanupFailureCollector
    ) {
        failures.add(
            registration.name,
            registration.failureBoundary,
            LifecycleState.CLOSING,
            CleanupFailureCategory.RESOURCE_CLOSE,
            failure
        )
        LifecycleFailurePolicy.report(
            LifecycleResourceName("resource-close:${registration.name.value}"), registration.failureBoundary, failure,
            "Failed to close server resource ${registration.name.value}"
        )
    }
}


