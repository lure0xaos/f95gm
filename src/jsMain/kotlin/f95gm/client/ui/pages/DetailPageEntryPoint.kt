package f95gm.client.ui.pages

import dev.fritz2.core.RenderContext
import f95gm.client.state.runtime.ClientDetailUiPort
import f95gm.domain.values.F95ThreadId

/** Page entry point kept separate from the detail rendering implementation. */
internal fun RenderContext.detailPageContent(runtime: ClientDetailUiPort, threadId: F95ThreadId) {
    renderDetailPageContent(runtime, threadId)
}
