package f95gm.server.state.runtime

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Immutable shutdown policy kept separate from lifecycle registration mechanics. */
internal data class LifecyclePolicy(val shutdownTimeout: Duration = 10.seconds) {
    init { require(shutdownTimeout.isPositive()) }
}
