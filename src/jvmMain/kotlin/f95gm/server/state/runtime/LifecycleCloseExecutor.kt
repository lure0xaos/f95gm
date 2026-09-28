package f95gm.server.state.runtime

/** Executes an already dependency-ordered close plan in either lifecycle mode. */
internal object LifecycleCloseExecutor {
    fun closeSynchronously(
        registrations: List<LifecycleRegistrationStore.Registration>,
        failures: CleanupFailureCollector,
        recordFailure: (LifecycleRegistrationStore.Registration, Exception, CleanupFailureCollector) -> Unit
    ) = registrations.forEach { registration ->
        closeSafely(registration.resource) { failure -> recordFailure(registration, failure, failures) }
    }

    suspend fun closeAwaitably(
        registrations: List<LifecycleRegistrationStore.Registration>,
        failures: CleanupFailureCollector,
        recordFailure: (LifecycleRegistrationStore.Registration, Exception, CleanupFailureCollector) -> Unit
    ) = registrations.forEach { registration ->
        closeAwaitablySafely({ registration.awaitable?.closeAndJoin() ?: registration.resource.close() }) { failure ->
            recordFailure(registration, failure, failures)
        }
    }
}
