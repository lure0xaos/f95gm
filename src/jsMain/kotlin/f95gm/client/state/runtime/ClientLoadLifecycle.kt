package f95gm.client.state.runtime

internal data class ClientLoadLifecycle(
    val statuses: Map<ClientLoadState, ClientLoadStatus> = emptyMap()
) {
    fun status(state: ClientLoadState): ClientLoadStatus = statuses[state] ?: ClientLoadStatus.NOT_LOADED
    fun markLoaded(state: ClientLoadState): ClientLoadLifecycle =
        copy(statuses = statuses + (state to ClientLoadStatus.LOADED))
    fun clear(state: ClientLoadState): ClientLoadLifecycle =
        copy(statuses = statuses + (state to ClientLoadStatus.NOT_LOADED))
}
