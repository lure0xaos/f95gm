package f95gm.server.cache

import f95gm.server.kernel.AwaitableLifecycleResource

/** Common lifecycle seam for blocking and suspendable cache-load coordinators. */
internal interface CacheLoadLifecycle : AwaitableLifecycleResource {
    /** Cancels queued waiters while keeping the coordinator reusable. */
    fun clear()

    /** Permanently rejects new work and begins terminal shutdown. */
    override fun close()
}
