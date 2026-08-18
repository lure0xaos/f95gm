package f95gm.server.model.detail

import f95gm.domain.values.*
import f95gm.server.state.runtime.upstreamJson
import kotlinx.serialization.Serializable

@Serializable
internal data class ThreadDetail(
    val threadId: F95ThreadId,
    val title: F95GameTitle,
    val creator: F95DeveloperName,
    val date: F95DateLabel,
    val cover: F95CoverUrl,
    val meta: List<DetailMeta>,
    val overviewHtml: F95Html,
    val sections: List<DetailSection>,
    val bodyHtml: F95Html
) {
    fun toJson(): String = upstreamJson.encodeToString(this)
}
