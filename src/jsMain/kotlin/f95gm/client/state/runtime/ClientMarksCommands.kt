package f95gm.client.state.runtime

import f95gm.client.actions.marks.loading.MarkCheckNotification

import f95gm.client.model.marks.GameMarkDraft
import f95gm.client.model.marks.MarkedGame
import f95gm.client.ui.shared.pagination.PageNumber
import f95gm.domain.filters.F95Sort
import f95gm.domain.tracking.GameTrackingState
import f95gm.domain.values.F95ThreadId
import f95gm.client.actions.marks.editing.selectMarkedSort as selectMarkedSortFromEditing
import f95gm.client.actions.marks.editing.updateMarkedSearch as updateMarkedSearchFromEditing
import f95gm.client.actions.marks.loading.checkMarkedGames as checkMarkedGamesFromLoading
import f95gm.client.actions.marks.loading.loadMarksIfNeeded as loadMarksIfNeededFromLoading
import f95gm.client.actions.marks.loading.retryMarkedGames as retryMarkedGamesFromLoading
import f95gm.client.actions.marks.mutations.saveGameMark as saveGameMarkFromMutations
import f95gm.client.actions.marks.mutations.saveMarkedTrackingState as saveMarkedTrackingStateFromMutations
import f95gm.client.actions.marks.mutations.unmarkGame as unmarkGameFromMutations
import f95gm.client.actions.marks.pagination.closeMarkedPageEdit as closeMarkedPageEditFromPagination
import f95gm.client.actions.marks.pagination.moveMarkedPage as moveMarkedPageFromPagination
import f95gm.client.actions.marks.pagination.moveMarkedPageTo as moveMarkedPageToFromPagination
import f95gm.client.actions.marks.pagination.startMarkedPageEdit as startMarkedPageEditFromPagination
import f95gm.client.actions.marks.pagination.submitMarkedPageInput as submitMarkedPageInputFromPagination
import f95gm.client.actions.marks.pagination.updateMarkedPageInput as updateMarkedPageInputFromPagination

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
    override fun selectMarkedSort(sort: F95Sort) = selectMarkedSortFromEditing(actions, sort)
    override fun updateMarkedSearch(search: String) = updateMarkedSearchFromEditing(actions, search)
    override fun checkMarkedGames(notification: MarkCheckNotification) = checkMarkedGamesFromLoading(actions, notification)
    override fun loadMarksIfNeeded() = loadMarksIfNeededFromLoading(actions)
    override fun retryMarkedGames() = retryMarkedGamesFromLoading(actions)
    override fun saveGameMark(draft: GameMarkDraft) = saveGameMarkFromMutations(actions, draft)
    override fun saveMarkedTrackingState(mark: MarkedGame, trackingState: GameTrackingState, acknowledge: Boolean) = saveMarkedTrackingStateFromMutations(actions, mark, trackingState, acknowledge)
    override fun unmarkGame(threadId: F95ThreadId) = unmarkGameFromMutations(actions, threadId)
    override fun closeMarkedPageEdit() = closeMarkedPageEditFromPagination(actions)
    override fun moveMarkedPage(delta: Int) = moveMarkedPageFromPagination(actions, delta)
    override fun moveMarkedPageTo(page: Int) = moveMarkedPageToFromPagination(actions, page)
    override fun moveMarkedPageTo(page: PageNumber) = moveMarkedPageToFromPagination(actions, page)
    override fun startMarkedPageEdit() = startMarkedPageEditFromPagination(actions)
    override fun submitMarkedPageInput() = submitMarkedPageInputFromPagination(actions)
    override fun updateMarkedPageInput(input: String) = updateMarkedPageInputFromPagination(actions, input)
}
