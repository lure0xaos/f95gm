package f95gm.server.kernel

internal interface LifecycleResource : AutoCloseable {
    val isClosed: Boolean
}

/** A lifecycle resource that declares the owner responsible for its lifetime. */
internal interface OwnedLifecycleResource : LifecycleResource {
    val owner: LifecycleResourceName
}

internal interface AwaitableLifecycleResource : LifecycleResource {
    suspend fun closeAndJoin()
}

/** Canonical names used by the lifecycle graph and its diagnostics. */
internal object LifecycleResources {
    val SERVER = LifecycleResourceName("server")
    val CONFIGURATION = LifecycleResourceName("configuration")
    val RESOURCES = LifecycleResourceName("resources")
    val UPSTREAM = LifecycleResourceName("upstream")
    val ROUTES = LifecycleResourceName("routes")
    val SESSION_EVICTION = LifecycleResourceName("session-eviction")
}

@JvmInline
internal value class LifecycleResourceName(val value: String) {
    init { require(value.isNotBlank()) }
}
