package f95gm.server.state.runtime

internal object SystemServerClock : ServerClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
    override fun monotonicNanos(): Long = System.nanoTime()
}
