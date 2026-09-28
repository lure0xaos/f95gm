package f95gm.client.state.runtime

import f95gm.client.actions.marks.loading.MarkCheckNotification

import f95gm.client.model.marks.GameMarkDraft
import f95gm.client.model.marks.MarkedGame
import f95gm.client.ui.shared.pagination.PageNumber
import f95gm.domain.filters.F95Sort
import f95gm.domain.tracking.GameTrackingState
import f95gm.domain.values.F95ThreadId

/** UI commands expose intent without writable stores or transport access. */
internal interface ClientMarksCommands {
    fun selectMarkedSort(sort: F95Sort)
    fun updateMarkedSearch(search: String)
    fun checkMarkedGames(notification: MarkCheckNotification)
    fun loadMarksIfNeeded()
    fun retryMarkedGames()
    fun saveGameMark(draft: GameMarkDraft)
    fun saveMarkedTrackingState(mark: MarkedGame, trackingState: GameTrackingState, acknowledge: Boolean = false)
    fun unmarkGame(threadId: F95ThreadId)
    fun closeMarkedPageEdit()
    fun moveMarkedPage(delta: Int)
    fun moveMarkedPageTo(page: Int)
    fun moveMarkedPageTo(page: PageNumber)
    fun startMarkedPageEdit()
    fun submitMarkedPageInput()
    fun updateMarkedPageInput(input: String)
}

internal class ClientMarksCommandsAdapter(private val actions: ClientMarksActionsPort) : ClientMarksCommands {
    override fun selectMarkedSort(sort: F95Sort) = f95gm.client.actions.marks.editing.selectMarkedSort(actions, sort)
    override fun updateMarkedSearch(search: String) = f95gm.client.actions.marks.editing.updateMarkedSearch(actions, search)
    override fun checkMarkedGames(notification: MarkCheckNotification) = f95gm.client.actions.marks.loading.checkMarkedGames(actions, notification)
    override fun loadMarksIfNeeded() = f95gm.client.actions.marks.loading.loadMarksIfNeeded(actions)
    override fun retryMarkedGames() = f95gm.client.actions.marks.loading.retryMarkedGames(actions)
    override fun saveGameMark(draft: GameMarkDraft) = f95gm.client.actions.marks.mutations.saveGameMark(actions, draft)
    override fun saveMarkedTrackingState(mark: MarkedGame, trackingState: GameTrackingState, acknowledge: Boolean) = f95gm.client.actions.marks.mutations.saveMarkedTrackingState(actions, mark, trackingState, acknowledge)
    override fun unmarkGame(threadId: F95ThreadId) = f95gm.client.actions.marks.mutations.unmarkGame(actions, threadId)
    override fun closeMarkedPageEdit() = f95gm.client.actions.marks.pagination.closeMarkedPageEdit(actions)
    override fun moveMarkedPage(delta: Int) = f95gm.client.actions.marks.pagination.moveMarkedPage(actions, delta)
    override fun moveMarkedPageTo(page: Int) = f95gm.client.actions.marks.pagination.moveMarkedPageTo(actions, page)
    override fun moveMarkedPageTo(page: PageNumber) = f95gm.client.actions.marks.pagination.moveMarkedPageTo(actions, page)
    override fun startMarkedPageEdit() = f95gm.client.actions.marks.pagination.startMarkedPageEdit(actions)
    override fun submitMarkedPageInput() = f95gm.client.actions.marks.pagination.submitMarkedPageInput(actions)
    override fun updateMarkedPageInput(input: String) = f95gm.client.actions.marks.pagination.updateMarkedPageInput(actions, input)
}
