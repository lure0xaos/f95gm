package f95gm.server.state.runtime

import f95gm.server.config.logging.logger

/** Owns server resources and closes them in reverse registration order. */
internal class ServerLifecycle : AutoCloseable {
    private val lock = Any()
    private val resources = mutableListOf<AutoCloseable>()
    private var closed = false

    fun <T : AutoCloseable> register(resource: T): T {
        val closeImmediately = synchronized(lock) {
            if (closed) true else {
                resources += resource
                false
            }
        }
        if (closeImmediately) resource.close()
        return resource
    }

    val isClosed: Boolean get() = synchronized(lock) { closed }

    override fun close() {
        val toClose = synchronized(lock) {
            if (closed) return
            closed = true
            resources.asReversed().toList().also { resources.clear() }
        }
        val failures = mutableListOf<Exception>()
        toClose.forEach { resource ->
            try {
                resource.close()
            } catch (error: Exception) {
                failures += error
                logger.warn(error) { "Failed to close server resource ${resource::class.simpleName}" }
            }
        }
        if (failures.isNotEmpty()) {
            throw ServerLifecycleCloseException(failures)
        }
    }
}
