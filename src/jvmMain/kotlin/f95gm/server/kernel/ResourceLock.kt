package f95gm.server.kernel

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class ResourceLock {
    @PublishedApi
    internal val delegate = ReentrantLock()

    inline fun <T> withLock(action: () -> T): T = delegate.withLock(action)
}
