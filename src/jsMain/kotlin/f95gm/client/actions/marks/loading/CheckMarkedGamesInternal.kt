package f95gm.client.actions.marks.loading

import f95gm.client.actions.feedback.showToast
import f95gm.client.data.api.apiResult
import f95gm.client.data.api.apiUserMessage
import f95gm.client.data.api.ApiFallbacks
import f95gm.client.model.marks.markedGamesView
import f95gm.client.state.runtime.ClientMarksActionsPort
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.first
import f95gm.client.state.runtime.AsyncState

internal suspend fun checkMarkedGamesInternal(runtime: ClientMarksActionsPort, notification: MarkCheckNotification) {
    val marks = runtime.marks
    marks.enqueue { it.copy(async = AsyncState.Loading) }
    apiResult {
        runtime.marksApi.marks()
    }.onSuccess { checked ->
        val marked = checked.map { game ->
            game.copy(isNew = runtime.seenCatalogThreadIds.markNew(game.threadId, game.isNew))
        }
        runtime.seenCatalogThreadIds.rememberThreadIds(checked.map { it.threadId })
        val previous = marks.data.first().items.associateBy { it.threadId }
        marks.enqueue { current ->
            val next = current.copy(
                items = marked,
                async = AsyncState.Success(Unit)
            )
            next.copy(page = markedGamesView(next).page)
        }
        if (notification == MarkCheckNotification.SHOW_TOAST) {
            val newlyAvailable = marked.filter {
                it.updateAvailable && previous[it.threadId]?.updateAvailable != true
            }
            if (newlyAvailable.isNotEmpty()) {
                val title = newlyAvailable.first().title
                showToast(runtime,
                    if (newlyAvailable.size == 1) UiMessages.app_newerVersionSingle(
                        title.value,
                        newlyAvailable.first().latestVersion.value
                    )
                    else UiMessages.app_newerVersionMultiple(newlyAvailable.size)
                )
            }
        }
    }.onFailure { error ->
        marks.enqueue {
            it.copy(
                async = AsyncState.Failure(error.apiUserMessage(ApiFallbacks.MarkedGames))
            )
        }
    }
}
