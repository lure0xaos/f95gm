package f95gm.server.platform

import f95gm.server.kernel.ServerClock
import f95gm.server.kernel.MonotonicMillis
import f95gm.server.kernel.MonotonicNanos
import f95gm.server.model.EpochMillis

internal object SystemServerClock : ServerClock {
    override fun epochMillis() = EpochMillis(System.currentTimeMillis())
    override fun monotonicTimestamp() = MonotonicNanos(System.nanoTime())
    override fun monotonicMillisTimestamp() = MonotonicMillis(monotonicTimestamp().value / 1_000_000L)
}
