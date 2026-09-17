package f95gm.server.config

/** Explicit observer boundary for dynamic configuration changes. */
internal fun interface ConfigurationReloadListener {
    fun onReload(result: ConfigurationReloadResult)
}

internal fun interface ConfigurationListenerFailureHandler {
    fun onFailure(listener: ConfigurationReloadListener, failure: Exception)
}

internal fun interface ConfigurationEventSinkFailureHandler {
    fun onFailure(failure: Exception)
}

/** Keeps configuration state independent from the concrete logging implementation. */
internal interface ConfigurationReloadEventSink {
    fun onReload(result: ConfigurationReloadResult)
    fun onUnchanged(source: String)
}

internal object NoopConfigurationReloadEventSink : ConfigurationReloadEventSink {
    override fun onReload(result: ConfigurationReloadResult) = Unit
    override fun onUnchanged(source: String) = Unit
}
