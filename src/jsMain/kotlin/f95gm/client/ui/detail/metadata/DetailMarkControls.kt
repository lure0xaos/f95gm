package f95gm.client.ui.detail.metadata

import dev.fritz2.core.RenderContext
import dev.fritz2.core.type
import f95gm.client.model.catalog.DropdownChoice
import f95gm.client.model.detail.DetailState
import f95gm.client.model.marks.GameMarkDraft
import f95gm.client.state.runtime.ClientMarkControlsUiPort
import f95gm.client.state.options.trackingStateOptions
import f95gm.client.ui.shared.dropdown.bootstrapDropdown
import f95gm.client.ui.shared.navigation.navigationButtonBaseClass
import f95gm.client.ui.shared.navigation.primaryNavigationButtonClass
import f95gm.domain.tracking.GameTrackingState
import f95gm.domain.values.StoredVersion
import f95gm.messages.UiMessages
import kotlinx.coroutines.flow.flowOf

internal fun RenderContext.detailMarkControls(runtime: ClientMarkControlsUiPort, state: DetailState) {
    val marks = runtime.marksView
    val currentVersion = state.meta.firstOrNull { it.label.value.equals(UiMessages.app_version(), ignoreCase = true) }
        ?.value?.value
        ?.takeIf { it.isNotBlank() }
        ?: UiMessages.app_versionUnknown()
    marks.data.render { markState ->
        val mark = markState.items.firstOrNull { it.threadId == state.threadId }
        val selectedTrackingState = mark?.trackingState?.wireValue.orEmpty()
        div("mark-controls d-flex flex-wrap align-items-center gap-2 mt-2") {
            bootstrapDropdown(
                runtime = runtime,
                selected = flowOf(selectedTrackingState),
                selectedLabel = flowOf(
                    trackingStateOptions.firstOrNull { it.first.wireValue == selectedTrackingState }?.second
                        ?: UiMessages.app_trackingStateNotMarked()
                ),
                options = flowOf(
                    listOf(
                        DropdownChoice(
                            "",
                            UiMessages.app_trackingStateNotMarked()
                        )
                    ) + trackingStateOptions.map { DropdownChoice(it.first.wireValue, it.second) }),
                buttonClass = "$navigationButtonBaseClass badge rounded-pill ${if (selectedTrackingState.isBlank()) "text-bg-secondary" else "text-bg-primary"}",
                widthClass = "w-auto"
            ) { selected ->
                if (selected.isBlank()) runtime.marksCommands.unmarkGame(state.threadId)
                else GameTrackingState.fromWire(selected)?.let { trackingState ->
                    runtime.marksCommands.saveGameMark(GameMarkDraft(
                            threadId = state.threadId,
                            storedVersion = StoredVersion(currentVersion),
                            trackingState = trackingState
                        )
                    )
                }
            }
            if (mark?.updateAvailable == true) {
                span("update-badge badge text-bg-warning text-wrap text-start") {
                    +UiMessages.app_newVersionAvailable(
                        mark.latestVersion.value
                    )
                }
                button("download-button $primaryNavigationButtonClass") {
                    type("button")
                    +UiMessages.app_markNewVersionDownloaded()
                    clicks handledBy {
                        runtime.marksCommands.saveGameMark(GameMarkDraft(
                                threadId = state.threadId,
                                storedVersion = StoredVersion(mark.latestVersion.value),
                                trackingState = GameTrackingState.DOWNLOADED,
                                acknowledge = true
                            )
                        )
                    }
                }
            }
        }
    }
}
