package f95gm.server.state.runtime

internal fun elapsedAtLeast(now: Long, startedAt: Long, durationMillis: Long): Boolean =
    now >= startedAt && now - startedAt >= durationMillis

internal fun saturatingAdd(left: Long, right: Long): Long =
    if (right > 0L && left > Long.MAX_VALUE - right) Long.MAX_VALUE else left + right

internal fun saturatingDouble(value: Long): Long =
    if (value > Long.MAX_VALUE / 2L) Long.MAX_VALUE else value * 2L

internal fun saturatingMultiply(left: Long, right: Long): Long =
    if (left == 0L || right == 0L) 0L
    else if (left > Long.MAX_VALUE / right) Long.MAX_VALUE
    else left * right
