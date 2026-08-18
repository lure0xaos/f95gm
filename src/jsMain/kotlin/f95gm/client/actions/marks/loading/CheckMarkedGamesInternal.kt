package f95gm.client.actions.marks.loading

import f95gm.client.actions.feedback.showToast
import f95gm.client.data.api.getJson
import f95gm.client.data.marks.parseMarks
import f95gm.client.model.marks.markedGamesView
import f95gm.client.state.catalog.SeenCatalogThreadIds
import f95gm.client.state.marks.marks
import f95gm.domain.api.F95Api
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.first

internal suspend fun checkMarkedGamesInternal(notify: Boolean) {
    marks.enqueue { it.copy(loading = it.items.isEmpty(), checking = true, error = "") }
    runCatching {
        parseMarks(getJson(F95Api.MARKS_CHECK))
    }.onSuccess { checked ->
        val marked = checked.map { game ->
            game.copy(isNew = SeenCatalogThreadIds.markNew(game.threadId.value, game.isNew))
        }
        SeenCatalogThreadIds.rememberThreadIds(checked.map { it.threadId.value })
        val previous = marks.data.first().items.associateBy { it.threadId }
        marks.enqueue { current ->
            val next = current.copy(
                items = marked,
                loading = false,
                checking = false,
                error = ""
            )
            next.copy(page = markedGamesView(next).page)
        }
        if (notify) {
            val newlyAvailable = marked.filter {
                it.updateAvailable && previous[it.threadId]?.updateAvailable != true
            }
            if (newlyAvailable.isNotEmpty()) {
                val title = newlyAvailable.first().title
                showToast(
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
                loading = false,
                checking = false,
                error = error.message ?: UiMessages.app_markedGamesCheckFailed()
            )
        }
    }
}
