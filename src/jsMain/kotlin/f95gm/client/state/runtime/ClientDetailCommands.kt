package f95gm.client.state.runtime

import f95gm.domain.values.F95ThreadId

/** UI commands expose intent without writable stores or transport access. */
internal interface ClientDetailCommands {
    fun loadDetailIfNeeded(threadId: F95ThreadId)
    fun retryDetail(threadId: F95ThreadId)
}

internal class ClientDetailCommandsAdapter(private val actions: ClientDetailActionsPort) : ClientDetailCommands {
    override fun loadDetailIfNeeded(threadId: F95ThreadId) = f95gm.client.actions.detail.loadDetailIfNeeded(actions, threadId)
    override fun retryDetail(threadId: F95ThreadId) = f95gm.client.actions.detail.retryDetail(actions, threadId)
}
