package f95gm.server.state.runtime

import f95gm.server.kernel.ServerInvariantViolation

import f95gm.server.kernel.ValidatedState
import f95gm.server.kernel.ResourceLock

import f95gm.server.kernel.BoundaryFailure
import kotlinx.coroutines.CancellationException
import f95gm.server.kernel.MetricCounter
import f95gm.server.kernel.LifecycleResource

internal enum class StartupStage { CREATED, CONFIGURED, DATABASE_READY, COMPOSED, SERVING, STOPPED, FAILED }

internal enum class StartupFailureCategory { CONFIGURATION, DATABASE, COMPOSITION, BINDING, RUNTIME }

internal class StartupFailure(
    val category: StartupFailureCategory,
    val stage: StartupStage,
    cause: Throwable
) : BoundaryFailure("Startup failed during ${stage.name.lowercase()} (${category.name.lowercase()})", cause)

internal class StartupInvariantViolation(message: String, cause: Throwable? = null) :
    ServerInvariantViolation(message, cause)

private data class StartupRollbackAction(
    val name: String,
    val resource: LifecycleResource
)

internal class StartupMetrics {
    val rollbackFailures = MetricCounter()
    val lifecycleRejections = MetricCounter()
}

internal class StartupRollbackStack(private val metrics: StartupMetrics = StartupMetrics()) {
    private val actions = ArrayDeque<StartupRollbackAction>()
    private val lock = ResourceLock()
    private var rolledBack = false

    fun register(name: String, resource: LifecycleResource) {
        lock.withLock {
            if (rolledBack) throw StartupInvariantViolation("Cannot register startup rollback after rollback has begun")
            actions.addLast(StartupRollbackAction(name, resource))
        }
    }

    @Suppress("TooGenericExceptionCaught") // Runtime graph construction must release resources acquired before any failure.
    fun <T> construct(block: (StartupRollbackStack) -> T): T {
        val result = try {
            block(this)
        } catch (failure: Exception) {
            rollback().forEach(failure::addSuppressed)
            throw failure
        } catch (fatal: Error) {
            rollback().forEach(fatal::addSuppressed)
            throw fatal
        }
        commit()
        return result
    }

    fun commit() {
        lock.withLock {
            check(!rolledBack) { "Cannot commit startup rollback after rollback has begun" }
            rolledBack = true
            actions.clear()
        }
    }

    @Suppress("TooGenericExceptionCaught") // Rollback aggregates ordinary cleanup failures and preserves cancellation/fatal errors.
    fun rollback(): List<Throwable> {
        val pending = lock.withLock {
            if (rolledBack) return emptyList()
            rolledBack = true
            actions.toList().asReversed().also { actions.clear() }
        }
        val failures = mutableListOf<Throwable>()
        pending.forEach { rollbackAction ->
            try {
                rollbackAction.resource.close()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                failures += StartupInvariantViolation("Startup rollback failed for ${rollbackAction.name}", failure)
                metrics.rollbackFailures.increment()
            }
        }
        return failures
    }
}

/** Monotonic startup state used to make partial initialization and rollback observable. */
internal class StartupState {
    private val stage = ValidatedState(StartupStage.CREATED, allowedTransitions)

    fun advance(next: StartupStage) = stage.transition(next)

    fun fail() = stage.transition(StartupStage.FAILED)

    fun stop() = stage.transition(StartupStage.STOPPED)

    fun current(): StartupStage = stage.current()

    private companion object {
        val allowedTransitions: Map<StartupStage, Set<StartupStage>> = mapOf(
            StartupStage.CREATED to setOf(StartupStage.CONFIGURED, StartupStage.FAILED),
            StartupStage.CONFIGURED to setOf(StartupStage.DATABASE_READY, StartupStage.FAILED),
            StartupStage.DATABASE_READY to setOf(StartupStage.COMPOSED, StartupStage.FAILED),
            StartupStage.COMPOSED to setOf(StartupStage.SERVING, StartupStage.FAILED),
            StartupStage.SERVING to setOf(StartupStage.STOPPED, StartupStage.FAILED),
            StartupStage.FAILED to setOf(StartupStage.STOPPED),
            StartupStage.STOPPED to emptySet()
        )
    }
}
