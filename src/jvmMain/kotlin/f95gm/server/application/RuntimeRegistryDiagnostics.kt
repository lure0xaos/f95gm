package f95gm.server.application

@JvmInline
internal value class RegistrySnapshotSequence(val value: Long) {
    init { require(value >= 0L) }
}

internal data class RuntimeRegistryDiagnostics(
    val sessionEntries: Int = 0,
    val idempotencyEntries: Int = 0,
    val sampleSequence: RegistrySnapshotSequence = RegistrySnapshotSequence(0),
    val consistent: Boolean = false
)
