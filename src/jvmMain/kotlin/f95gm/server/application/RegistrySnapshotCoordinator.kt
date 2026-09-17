package f95gm.server.application

import f95gm.server.kernel.ResourceLock

/** Coordinates point-in-time diagnostics across registries that share one runtime. */
internal class RegistrySnapshotCoordinator {
    private val lock = ResourceLock()
    private var sequence = 0L

    internal inline fun <T> withLock(block: () -> T): T = lock.withLock(block)

    fun snapshot(sessions: SessionRegistry, idempotency: IdempotencyRegistry): RuntimeRegistryDiagnostics =
        lock.withLock {
            sequence += 1
            RuntimeRegistryDiagnostics(
                sessionEntries = sessions.size,
                idempotencyEntries = idempotency.size,
                sampleSequence = RegistrySnapshotSequence(sequence),
                consistent = true
            )
        }
}
