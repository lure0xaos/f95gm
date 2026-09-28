package f95gm.server.platform

import f95gm.server.kernel.ServerClock

internal object SystemServerClock : ServerClock {
    override fun epochMillis() = f95gm.server.model.EpochMillis(System.currentTimeMillis())
    override fun monotonicTimestamp() = f95gm.server.kernel.MonotonicNanos(System.nanoTime())
    override fun monotonicMillisTimestamp() = f95gm.server.kernel.MonotonicMillis(monotonicTimestamp().value / 1_000_000L)
}
