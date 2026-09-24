package f95gm.server.state.runtime

internal interface LifecycleResource : AutoCloseable {
    val isClosed: Boolean
}
