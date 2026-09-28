package f95gm.server.kernel

import f95gm.server.model.EpochMillis
import kotlin.time.Duration

internal fun elapsedAtLeast(now: Long, startedAt: Long, durationMillis: Long): Boolean =
    now >= startedAt && now - startedAt >= durationMillis

internal fun elapsedAtLeast(now: EpochMillis, startedAt: EpochMillis, durationMillis: Long): Boolean =
    elapsedAtLeast(now.value, startedAt.value, durationMillis)

internal fun elapsedAtLeast(now: MonotonicNanos, startedAt: MonotonicNanos, duration: Duration): Boolean =
    now.value >= startedAt.value && now.value - startedAt.value >= duration.inWholeNanoseconds

internal fun saturatingAdd(left: Long, right: Long): Long =
    if (right > 0L && left > Long.MAX_VALUE - right) Long.MAX_VALUE else left + right

internal fun saturatingDouble(value: Long): Long =
    if (value > Long.MAX_VALUE / 2L) Long.MAX_VALUE else value * 2L

internal fun saturatingMultiply(left: Long, right: Long): Long =
    if (left == 0L || right == 0L) 0L
    else if (left > Long.MAX_VALUE / right) Long.MAX_VALUE
    else left * right
