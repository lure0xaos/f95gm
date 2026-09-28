package f95gm.client.state.runtime

internal fun interface ClientOperationStatusSink {
    fun publish(key: ClientJobKey, status: ClientOperationStatus)
}
