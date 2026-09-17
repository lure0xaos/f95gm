package f95gm.domain.values

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class F95EpochMillis(val value: Long) : Comparable<F95EpochMillis> {
    init {
        require(value >= 0) { "Epoch timestamp must not be negative" }
    }

    override fun compareTo(other: F95EpochMillis): Int = value.compareTo(other.value)
}
