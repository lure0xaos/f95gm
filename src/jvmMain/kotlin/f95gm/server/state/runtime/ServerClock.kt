package f95gm.server.state.runtime

internal interface ServerClock {
    fun nowMillis(): Long
    fun monotonicNanos(): Long = System.nanoTime()
}
