package f95gm.server.model

import kotlinx.serialization.Serializable

/** A wall-clock timestamp stored as Unix epoch milliseconds. */
@JvmInline
@Serializable
internal value class EpochMillis(val value: Long) : Comparable<EpochMillis> {
    init {
        require(value >= 0) { "Epoch timestamp must not be negative" }
    }

    override fun compareTo(other: EpochMillis): Int = value.compareTo(other.value)
}
