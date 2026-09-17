package f95gm.client.model.detail

import f95gm.domain.values.*
import f95gm.client.state.runtime.AsyncState
import f95gm.client.state.runtime.isLoading
import f95gm.client.state.runtime.errorMessage

internal data class DetailState(
    val threadId: F95ThreadId = F95ThreadId(""),
    val title: F95GameTitle = F95GameTitle(""),
    val creator: F95DeveloperName = F95DeveloperName(""),
    val date: F95DateLabel = F95DateLabel(""),
    val cover: F95CoverUrl = F95CoverUrl(""),
    val meta: List<DetailMeta> = emptyList(),
    val overviewHtml: F95Html = F95Html(""),
    val sections: List<DetailSection> = emptyList(),
    val bodyHtml: F95Html = F95Html(""),
    val async: AsyncState<Unit> = AsyncState.Idle
)

internal val DetailState.loading: Boolean get() = async.isLoading
internal val DetailState.error: String get() = async.errorMessage
